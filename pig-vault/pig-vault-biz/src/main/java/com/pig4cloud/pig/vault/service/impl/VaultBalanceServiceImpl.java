/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.vault.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.vault.api.dto.BalanceResponse;
import com.pig4cloud.pig.vault.api.dto.DepositRequest;
import com.pig4cloud.pig.vault.api.entity.Balance;
import com.pig4cloud.pig.vault.api.entity.LedgerEntry;
import com.pig4cloud.pig.vault.api.entity.VaultAccount;
import com.pig4cloud.pig.vault.api.entity.VaultAsset;
import com.pig4cloud.pig.vault.api.enums.Direction;
import com.pig4cloud.pig.vault.api.enums.LedgerType;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.mapper.BalanceMapper;
import com.pig4cloud.pig.vault.mapper.LedgerEntryMapper;
import com.pig4cloud.pig.vault.mapper.VaultAccountMapper;
import com.pig4cloud.pig.vault.mapper.VaultAssetMapper;
import com.pig4cloud.pig.vault.service.VaultBalanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Vault Balance Service Implementation
 *
 * @author luka
 * @date 2025-01-23
 */
@Service
@RequiredArgsConstructor
public class VaultBalanceServiceImpl implements VaultBalanceService {

	private static final Logger log = LoggerFactory.getLogger(VaultBalanceServiceImpl.class);

	private final BalanceMapper balanceMapper;

	private final LedgerEntryMapper ledgerEntryMapper;

	private final VaultAssetMapper vaultAssetMapper;

	private final VaultAccountMapper vaultAccountMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BalanceResponse deposit(DepositRequest request) {
		// 0) Get account by userId
		VaultAccount account = vaultAccountMapper
			.selectOne(Wrappers.<VaultAccount>lambdaQuery().eq(VaultAccount::getUserId, request.getUserId()));
		if (account == null) {
			throw new IllegalStateException("Account not found for userId=" + request.getUserId());
		}
		Long accountId = account.getAccountId();

		// 1) Convert symbol to assetId
		Long assetId = getAssetIdBySymbol(request.getSymbol());

		// 2) Idempotency: try insert ledger entry (unique constraint on idempotencyKey)
		String idempotencyKey = buildIdempotencyKey(request.getRefId());
		LedgerEntry ledger = new LedgerEntry();
		ledger.setAccountId(accountId);
		ledger.setAssetId(assetId);
		ledger.setEntryType(LedgerType.DEPOSIT);
		ledger.setDirection(Direction.CREDIT);
		ledger.setAmount(request.getAmount());
		ledger.setIdempotencyKey(idempotencyKey);
		ledger.setRefType(RefType.DEPOSIT);
		ledger.setRefId(request.getRefId());

		try {
			// Try to insert the ledger entry first for idempotency
			// If it already exists, this will throw DuplicateKeyException
			ledgerEntryMapper.insert(ledger);
		}
		catch (DuplicateKeyException e) {
			// Idempotent: deposit already processed, return existing balance
			log.info("Deposit already processed for refId={}", request.getRefId());
			return getBalanceResponse(accountId, assetId, request.getSymbol());
		}

		// 3) Get balance with FOR UPDATE semantics (pessimistic lock)
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, accountId)
			.eq(Balance::getAssetId, assetId)
			.last("FOR UPDATE"));

		// If balance does not exist, create balance record
		if (balance == null) {
			log.info("Balance not found, creating new balance: userId={}, accountId={}, symbol={}, assetId={}",
					request.getUserId(), accountId, request.getSymbol(), assetId);

			// Create new balance record
			balance = new Balance();
			balance.setAccountId(accountId);
			balance.setAssetId(assetId);
			balance.setAvailable(BigDecimal.ZERO);
			balance.setFrozen(BigDecimal.ZERO);
			balance.setVersion(0L);

			balanceMapper.insert(balance);
			log.info("New balance record created: balanceId={}, accountId={}, assetId={}", balance.getBalanceId(),
					accountId, assetId);
		}

		// Store before values for audit trail
		BigDecimal beforeAvailable = balance.getAvailable();
		BigDecimal beforeFrozen = balance.getFrozen();

		// 4) Update balance (optimistic lock with version)
		int rows = balanceMapper.depositBalance(accountId, assetId, request.getAmount(), balance.getVersion());

		if (rows != 1) {
			throw new IllegalStateException("Concurrency conflict when updating balance");
		}

		// 5) Update ledger entry with before/after values
		ledger.setBeforeAvailable(beforeAvailable);
		ledger.setBeforeFrozen(beforeFrozen);
		ledger.setAfterAvailable(beforeAvailable.add(request.getAmount()));
		ledger.setAfterFrozen(beforeFrozen);
		ledgerEntryMapper.updateById(ledger);

		log.info("Deposit completed successfully: userId={}, accountId={}, symbol={}, amount={}, refId={}",
				request.getUserId(), accountId, request.getSymbol(), request.getAmount(), request.getRefId());

		// 6) Return updated balance
		return getBalanceResponse(accountId, assetId, request.getSymbol());
	}

	/**
	 * Get asset ID by symbol
	 */
	private Long getAssetIdBySymbol(String symbol) {
		VaultAsset asset = vaultAssetMapper.selectOne(
				Wrappers.<VaultAsset>lambdaQuery().eq(VaultAsset::getSymbol, symbol).eq(VaultAsset::getIsActive, true));

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found or inactive for symbol: " + symbol);
		}

		return asset.getAssetId();
	}

	/**
	 * Build idempotency key for ledger entry
	 */
	private String buildIdempotencyKey(String refId) {
		return LedgerType.DEPOSIT.name() + ":" + RefType.DEPOSIT.name() + ":" + refId;
	}

	/**
	 * Get balance response
	 */
	private BalanceResponse getBalanceResponse(Long accountId, Long assetId, String symbol) {
		Balance balance = balanceMapper.selectOne(
				Wrappers.<Balance>lambdaQuery().eq(Balance::getAccountId, accountId).eq(Balance::getAssetId, assetId));

		if (balance == null) {
			throw new IllegalStateException("Balance not found for accountId=" + accountId + ", assetId=" + assetId);
		}

		BalanceResponse response = new BalanceResponse();
		response.setBalanceId(balance.getBalanceId());
		response.setAccountId(balance.getAccountId());
		response.setAssetId(balance.getAssetId());
		response.setSymbol(symbol);
		response.setAvailable(balance.getAvailable());
		response.setFrozen(balance.getFrozen());
		response.setUpdateTime(balance.getUpdateTime() != null ? balance.getUpdateTime().toEpochMilli() : null);

		return response;
	}

	@Override
	public BalanceResponse getMyBalance(String symbol) {
		// 1) Get current user
		PigUser user = SecurityUtils.getUser();
		if (user == null) {
			throw new IllegalStateException("User not authenticated");
		}

		// 2) Get account for current user
		VaultAccount account = vaultAccountMapper
			.selectOne(Wrappers.<VaultAccount>lambdaQuery().eq(VaultAccount::getUserId, user.getId()));

		if (account == null) {
			throw new IllegalStateException("Account not found for current user");
		}

		// 3) Convert symbol to assetId
		VaultAsset asset = vaultAssetMapper.selectOne(
				Wrappers.<VaultAsset>lambdaQuery().eq(VaultAsset::getSymbol, symbol).eq(VaultAsset::getIsActive, true));

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found or inactive for symbol: " + symbol);
		}

		// 4) Get balance
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, account.getAccountId())
			.eq(Balance::getAssetId, asset.getAssetId()));

		if (balance == null) {
			throw new IllegalStateException("Balance not found");
		}

		// 5) Build response
		BalanceResponse response = new BalanceResponse();
		response.setBalanceId(balance.getBalanceId());
		response.setAccountId(balance.getAccountId());
		response.setAssetId(balance.getAssetId());
		response.setSymbol(symbol);
		response.setAvailable(balance.getAvailable());
		response.setFrozen(balance.getFrozen());
		response.setUpdateTime(balance.getUpdateTime() != null ? balance.getUpdateTime().toEpochMilli() : null);

		log.info("Get balance for user: userId={}, accountId={}, symbol={}, available={}, frozen={}", user.getId(),
				account.getAccountId(), symbol, balance.getAvailable(), balance.getFrozen());

		return response;
	}

}
