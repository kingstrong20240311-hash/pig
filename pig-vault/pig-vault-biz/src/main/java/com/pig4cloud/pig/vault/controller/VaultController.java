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

package com.pig4cloud.pig.vault.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.vault.api.dto.*;
import com.pig4cloud.pig.vault.api.entity.Balance;
import com.pig4cloud.pig.vault.api.entity.Freeze;
import com.pig4cloud.pig.vault.api.entity.VaultAsset;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.mapper.BalanceMapper;
import com.pig4cloud.pig.vault.mapper.FreezeMapper;
import com.pig4cloud.pig.vault.mapper.VaultAssetMapper;
import com.pig4cloud.pig.vault.service.VaultBalanceService;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Vault Controller
 *
 * @author luka
 * @date 2025-01-14
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(description = "vault", name = "Vault Management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class VaultController {

	private final VaultFreezeService vaultFreezeService;

	private final VaultBalanceService vaultBalanceService;

	private final BalanceMapper balanceMapper;

	private final VaultAssetMapper vaultAssetMapper;

	private final FreezeMapper freezeMapper;

	/**
	 * Create a freeze (lock funds from available to frozen)
	 * @param request freeze request
	 * @return freeze response
	 */
	@PostMapping("/freeze/create")
	@Operation(summary = "Create Freeze", description = "Create a freeze to lock funds")
	public R<FreezeResponse> createFreeze(@Valid @RequestBody CreateFreezeRequest request) {
		try {
			FreezeResponse response = vaultFreezeService.createFreeze(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to create freeze: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Release a freeze (unlock funds from frozen back to available)
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	@PostMapping("/freeze/release")
	@Operation(summary = "Release Freeze", description = "Release a freeze to unlock funds")
	public R<FreezeResponse> releaseFreeze(@Valid @RequestBody FreezeLookupRequest request) {
		try {
			FreezeResponse response = vaultFreezeService.releaseFreeze(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to release freeze: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Claim a freeze (mark freeze as claimed by settlement process)
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	@PostMapping("/freeze/claim")
	@Operation(summary = "Claim Freeze", description = "Claim a freeze for settlement")
	public R<FreezeResponse> claimFreeze(@Valid @RequestBody FreezeLookupRequest request) {
		try {
			FreezeResponse response = vaultFreezeService.claimFreeze(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to claim freeze: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Consume a freeze (spend frozen funds, total decreases)
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	@PostMapping("/freeze/consume")
	@Operation(summary = "Consume Freeze", description = "Consume a freeze to spend funds")
	public R<FreezeResponse> consumeFreeze(@Valid @RequestBody FreezeLookupRequest request) {
		try {
			FreezeResponse response = vaultFreezeService.consumeFreeze(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to consume freeze: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Get balance for an account and asset
	 * @param accountId account ID
	 * @param symbol asset symbol
	 * @return balance response
	 */
	@GetMapping("/balance")
	@Operation(summary = "Get Balance", description = "Get balance for an account and asset")
	public R<BalanceResponse> getBalance(@RequestParam("accountId") Long accountId,
			@RequestParam("symbol") String symbol) {
		try {
			// Convert symbol to assetId
			VaultAsset asset = vaultAssetMapper.selectOne(Wrappers.<VaultAsset>lambdaQuery()
				.eq(VaultAsset::getSymbol, symbol)
				.eq(VaultAsset::getIsActive, true));

			if (asset == null) {
				return R.failed("Asset not found or inactive for symbol: " + symbol);
			}

			Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
				.eq(Balance::getAccountId, accountId)
				.eq(Balance::getAssetId, asset.getAssetId()));

			if (balance == null) {
				return R.failed("Balance not found");
			}

			BalanceResponse response = new BalanceResponse();
			response.setBalanceId(balance.getBalanceId());
			response.setAccountId(balance.getAccountId());
			response.setAssetId(balance.getAssetId());
			response.setSymbol(symbol);
			response.setAvailable(balance.getAvailable());
			response.setFrozen(balance.getFrozen());
			response.setUpdateTime(balance.getUpdateTime() != null ? balance.getUpdateTime().toEpochMilli() : null);

			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to get balance: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Get freeze by reference
	 * @param refId reference ID (e.g., order ID)
	 * @param refType reference type (e.g., ORDER)
	 * @return freeze details
	 */
	@GetMapping("/freeze")
	@Operation(summary = "Get Freeze", description = "Get freeze by reference ID and type")
	public R<FreezeDTO> getFreeze(@RequestParam("refId") String refId, @RequestParam("refType") String refType) {
		try {
			Freeze freeze = freezeMapper.selectOne(Wrappers.<Freeze>lambdaQuery()
				.eq(Freeze::getRefId, refId)
				.eq(Freeze::getRefType, RefType.valueOf(refType)));

			if (freeze == null) {
				return R.failed("Freeze not found");
			}

			return R.ok(toFreezeDTO(freeze));
		}
		catch (Exception e) {
			log.error("Failed to get freeze: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Convert Freeze entity to FreezeDTO
	 * @param freeze Freeze entity
	 * @return FreezeDTO
	 */
	private FreezeDTO toFreezeDTO(Freeze freeze) {
		FreezeDTO dto = new FreezeDTO();
		dto.setFreezeId(freeze.getFreezeId());
		dto.setAccountId(freeze.getAccountId());
		dto.setAssetId(freeze.getAssetId());
		dto.setAmount(freeze.getAmount());
		dto.setStatus(freeze.getStatus());
		dto.setRefType(freeze.getRefType());
		dto.setRefId(freeze.getRefId());
		dto.setVersion(freeze.getVersion());
		dto.setCreateTime(freeze.getCreateTime() != null ? freeze.getCreateTime().toEpochMilli() : null);
		dto.setUpdateTime(freeze.getUpdateTime() != null ? freeze.getUpdateTime().toEpochMilli() : null);
		dto.setClaimTime(freeze.getClaimTime() != null ? freeze.getClaimTime().toEpochMilli() : null);
		return dto;
	}

	/**
	 * Get balance for current logged-in user by asset symbol
	 * @param symbol asset symbol (e.g., USDC)
	 * @return balance response
	 */
	@GetMapping("/balance/me")
	@Operation(summary = "Get My Balance", description = "Get balance for current user and specified asset")
	public R<BalanceResponse> getMyBalance(@RequestParam("symbol") String symbol) {
		try {
			BalanceResponse response = vaultBalanceService.getMyBalance(symbol);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to get balance: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Deposit funds to an account (increase available balance)
	 * @param request deposit request
	 * @return balance response
	 */
	@PostMapping("/deposit")
	@Operation(summary = "Deposit Funds", description = "Deposit funds to increase available balance")
	public R<BalanceResponse> deposit(@Valid @RequestBody DepositRequest request) {
		try {
			BalanceResponse response = vaultBalanceService.deposit(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to deposit funds: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

}
