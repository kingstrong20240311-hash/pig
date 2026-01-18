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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.vault.api.dto.CreateFreezeRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.entity.Balance;
import com.pig4cloud.pig.vault.api.entity.Freeze;
import com.pig4cloud.pig.vault.api.entity.LedgerEntry;
import com.pig4cloud.pig.vault.api.enums.Direction;
import com.pig4cloud.pig.vault.api.enums.FreezeStatus;
import com.pig4cloud.pig.vault.api.enums.LedgerType;
import com.pig4cloud.pig.vault.mapper.BalanceMapper;
import com.pig4cloud.pig.vault.mapper.FreezeMapper;
import com.pig4cloud.pig.vault.mapper.LedgerEntryMapper;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vault Freeze Service Implementation
 *
 * @author luka
 * @date 2025-01-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VaultFreezeServiceImpl implements VaultFreezeService {

	private final FreezeMapper freezeMapper;

	private final BalanceMapper balanceMapper;

	private final LedgerEntryMapper ledgerEntryMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FreezeResponse createFreeze(CreateFreezeRequest request) {
		// 1) Idempotency: try insert freeze (unique constraint on refType + refId)
		Freeze freeze = new Freeze();
		freeze.setAccountId(request.getAccountId());
		freeze.setAssetId(request.getAssetId());
		freeze.setAmount(request.getAmount());
		freeze.setStatus(FreezeStatus.HELD);
		freeze.setRefType(request.getRefType());
		freeze.setRefId(request.getRefId());
		freeze.setVersion(0L);

		try {
			freezeMapper.insert(freeze);
		}
		catch (DuplicateKeyException e) {
			// Idempotent: freeze already exists, return existing one
			log.info("Freeze already exists for refType={}, refId={}", request.getRefType(), request.getRefId());
			Freeze existing = findFreezeByRef(request.getRefType(), request.getRefId());
			return toFreezeResponse(existing);
		}

		// 2) Get balance with FOR UPDATE semantics (pessimistic lock)
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, request.getAccountId())
			.eq(Balance::getAssetId, request.getAssetId())
			.last("FOR UPDATE"));

		if (balance == null) {
			throw new IllegalStateException(
					"Balance not found for accountId=" + request.getAccountId() + ", assetId=" + request.getAssetId());
		}

		// Check available >= amount
		if (balance.getAvailable().compareTo(request.getAmount()) < 0) {
			throw new IllegalStateException("Insufficient available balance");
		}

		// 3) Update balance (optimistic lock with version)
		int rows = balanceMapper.freezeBalance(request.getAccountId(), request.getAssetId(), request.getAmount(),
				balance.getVersion());

		if (rows != 1) {
			throw new IllegalStateException("Concurrency conflict when updating balance");
		}

		// 4) Write ledger entry
		LedgerEntry ledger = new LedgerEntry();
		ledger.setAccountId(request.getAccountId());
		ledger.setAssetId(request.getAssetId());
		ledger.setEntryType(LedgerType.FREEZE);
		ledger.setDirection(Direction.DEBIT);
		ledger.setAmount(request.getAmount());
		ledger.setIdempotencyKey(buildIdempotencyKey(LedgerType.FREEZE, request.getRefType(), request.getRefId()));
		ledger.setRefType(request.getRefType());
		ledger.setRefId(request.getRefId());
		ledger.setBeforeAvailable(balance.getAvailable());
		ledger.setBeforeFrozen(balance.getFrozen());
		ledger.setAfterAvailable(balance.getAvailable().subtract(request.getAmount()));
		ledger.setAfterFrozen(balance.getFrozen().add(request.getAmount()));

		ledgerEntryMapper.insert(ledger);

		log.info("Freeze created successfully: freezeId={}, refType={}, refId={}", freeze.getFreezeId(),
				request.getRefType(), request.getRefId());

		return toFreezeResponse(freeze);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FreezeResponse releaseFreeze(FreezeLookupRequest request) {
		// 1) Find freeze with FOR UPDATE
		Freeze freeze = findFreezeByRefForUpdate(request.getRefType(), request.getRefId());

		// Idempotent: if already released/consumed/canceled/expired, return current
		// state
		if (freeze.getStatus().isTerminal()) {
			log.info("Freeze already in terminal state: freezeId={}, status={}", freeze.getFreezeId(),
					freeze.getStatus());
			return toFreezeResponse(freeze);
		}

		// 2) Update freeze status to RELEASED
		freeze.setStatus(FreezeStatus.RELEASED);
		freezeMapper.updateById(freeze);

		// 3) Get balance and update (frozen -> available)
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, freeze.getAccountId())
			.eq(Balance::getAssetId, freeze.getAssetId())
			.last("FOR UPDATE"));

		if (balance == null) {
			throw new IllegalStateException(
					"Balance not found for accountId=" + freeze.getAccountId() + ", assetId=" + freeze.getAssetId());
		}

		int rows = balanceMapper.unfreezeBalance(freeze.getAccountId(), freeze.getAssetId(), freeze.getAmount(),
				balance.getVersion());

		if (rows != 1) {
			throw new IllegalStateException("Concurrency conflict when unfreezing balance");
		}

		// 4) Write ledger entry
		LedgerEntry ledger = new LedgerEntry();
		ledger.setAccountId(freeze.getAccountId());
		ledger.setAssetId(freeze.getAssetId());
		ledger.setEntryType(LedgerType.UNFREEZE);
		ledger.setDirection(Direction.CREDIT);
		ledger.setAmount(freeze.getAmount());
		ledger.setIdempotencyKey(buildIdempotencyKey(LedgerType.UNFREEZE, request.getRefType(), request.getRefId()));
		ledger.setRefType(request.getRefType());
		ledger.setRefId(request.getRefId());
		ledger.setBeforeAvailable(balance.getAvailable());
		ledger.setBeforeFrozen(balance.getFrozen());
		ledger.setAfterAvailable(balance.getAvailable().add(freeze.getAmount()));
		ledger.setAfterFrozen(balance.getFrozen().subtract(freeze.getAmount()));

		ledgerEntryMapper.insert(ledger);

		log.info("Freeze released successfully: freezeId={}, refType={}, refId={}", freeze.getFreezeId(),
				request.getRefType(), request.getRefId());

		return toFreezeResponse(freeze);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FreezeResponse claimFreeze(FreezeLookupRequest request) {
		// 1) Find freeze with FOR UPDATE
		Freeze freeze = findFreezeByRefForUpdate(request.getRefType(), request.getRefId());

		// If already claimed, return idempotent response
		if (freeze.getStatus() == FreezeStatus.CLAIMED) {
			log.info("Freeze already claimed: freezeId={}", freeze.getFreezeId());
			return toFreezeResponse(freeze);
		}

		// If in terminal state (not HELD), reject
		if (freeze.getStatus() != FreezeStatus.HELD) {
			throw new IllegalStateException(
					"Cannot claim freeze in state: " + freeze.getStatus() + ", freezeId=" + freeze.getFreezeId());
		}

		// 2) Update freeze status to CLAIMED
		freeze.setStatus(FreezeStatus.CLAIMED);
		freeze.setClaimTime(Instant.now());
		freezeMapper.updateById(freeze);

		// 3) Write ledger entry (no balance change, just audit log)
		LedgerEntry ledger = new LedgerEntry();
		ledger.setAccountId(freeze.getAccountId());
		ledger.setAssetId(freeze.getAssetId());
		ledger.setEntryType(LedgerType.CLAIM);
		ledger.setDirection(Direction.DEBIT); // Semantic: marking as claimed
		ledger.setAmount(freeze.getAmount());
		ledger.setIdempotencyKey(buildIdempotencyKey(LedgerType.CLAIM, request.getRefType(), request.getRefId()));
		ledger.setRefType(request.getRefType());
		ledger.setRefId(request.getRefId());

		// Get current balance for audit trail
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, freeze.getAccountId())
			.eq(Balance::getAssetId, freeze.getAssetId()));

		if (balance != null) {
			ledger.setBeforeAvailable(balance.getAvailable());
			ledger.setBeforeFrozen(balance.getFrozen());
			ledger.setAfterAvailable(balance.getAvailable());
			ledger.setAfterFrozen(balance.getFrozen());
		}

		ledgerEntryMapper.insert(ledger);

		log.info("Freeze claimed successfully: freezeId={}, refType={}, refId={}", freeze.getFreezeId(),
				request.getRefType(), request.getRefId());

		return toFreezeResponse(freeze);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FreezeResponse consumeFreeze(FreezeLookupRequest request) {
		// 1) Find freeze with FOR UPDATE
		Freeze freeze = findFreezeByRefForUpdate(request.getRefType(), request.getRefId());

		// Idempotent: if already consumed, return current state
		if (freeze.getStatus() == FreezeStatus.CONSUMED) {
			log.info("Freeze already consumed: freezeId={}", freeze.getFreezeId());
			return toFreezeResponse(freeze);
		}

		// If already released/canceled/expired, this is a conflict
		if (freeze.getStatus() == FreezeStatus.RELEASED || freeze.getStatus() == FreezeStatus.CANCELED
				|| freeze.getStatus() == FreezeStatus.EXPIRED) {
			throw new IllegalStateException(
					"Cannot consume freeze in state: " + freeze.getStatus() + ", freezeId=" + freeze.getFreezeId());
		}

		// 2) Update freeze status to CONSUMED
		freeze.setStatus(FreezeStatus.CONSUMED);
		freezeMapper.updateById(freeze);

		// 3) Get balance and consume (frozen -> spent, total decreases)
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, freeze.getAccountId())
			.eq(Balance::getAssetId, freeze.getAssetId())
			.last("FOR UPDATE"));

		if (balance == null) {
			throw new IllegalStateException(
					"Balance not found for accountId=" + freeze.getAccountId() + ", assetId=" + freeze.getAssetId());
		}

		int rows = balanceMapper.consumeFreeze(freeze.getAccountId(), freeze.getAssetId(), freeze.getAmount(),
				balance.getVersion());

		if (rows != 1) {
			throw new IllegalStateException("Concurrency conflict when consuming freeze");
		}

		// 4) Write ledger entry
		LedgerEntry ledger = new LedgerEntry();
		ledger.setAccountId(freeze.getAccountId());
		ledger.setAssetId(freeze.getAssetId());
		ledger.setEntryType(LedgerType.CONSUME);
		ledger.setDirection(Direction.DEBIT);
		ledger.setAmount(freeze.getAmount());
		ledger.setIdempotencyKey(buildIdempotencyKey(LedgerType.CONSUME, request.getRefType(), request.getRefId()));
		ledger.setRefType(request.getRefType());
		ledger.setRefId(request.getRefId());
		ledger.setBeforeAvailable(balance.getAvailable());
		ledger.setBeforeFrozen(balance.getFrozen());
		ledger.setAfterAvailable(balance.getAvailable());
		ledger.setAfterFrozen(balance.getFrozen().subtract(freeze.getAmount()));

		ledgerEntryMapper.insert(ledger);

		log.info("Freeze consumed successfully: freezeId={}, refType={}, refId={}", freeze.getFreezeId(),
				request.getRefType(), request.getRefId());

		return toFreezeResponse(freeze);
	}

	/**
	 * Find freeze by refType and refId
	 */
	private Freeze findFreezeByRef(com.pig4cloud.pig.vault.api.enums.RefType refType, String refId) {
		LambdaQueryWrapper<Freeze> wrapper = Wrappers.<Freeze>lambdaQuery()
			.eq(Freeze::getRefType, refType)
			.eq(Freeze::getRefId, refId);

		Freeze freeze = freezeMapper.selectOne(wrapper);
		if (freeze == null) {
			throw new IllegalArgumentException("Freeze not found for refType=" + refType + ", refId=" + refId);
		}
		return freeze;
	}

	/**
	 * Find freeze by refType and refId with FOR UPDATE
	 */
	private Freeze findFreezeByRefForUpdate(com.pig4cloud.pig.vault.api.enums.RefType refType, String refId) {
		LambdaQueryWrapper<Freeze> wrapper = Wrappers.<Freeze>lambdaQuery()
			.eq(Freeze::getRefType, refType)
			.eq(Freeze::getRefId, refId)
			.last("FOR UPDATE");

		Freeze freeze = freezeMapper.selectOne(wrapper);
		if (freeze == null) {
			throw new IllegalArgumentException("Freeze not found for refType=" + refType + ", refId=" + refId);
		}
		return freeze;
	}

	/**
	 * Build idempotency key for ledger entry
	 */
	private String buildIdempotencyKey(LedgerType ledgerType, com.pig4cloud.pig.vault.api.enums.RefType refType,
			String refId) {
		return ledgerType.name() + ":" + refType.name() + ":" + refId;
	}

	/**
	 * Convert Freeze entity to FreezeResponse DTO
	 */
	private FreezeResponse toFreezeResponse(Freeze freeze) {
		FreezeResponse response = new FreezeResponse();
		response.setFreezeId(freeze.getFreezeId());
		response.setStatus(freeze.getStatus());
		response.setAmount(freeze.getAmount());
		response.setClaimTime(freeze.getClaimTime());
		return response;
	}

}
