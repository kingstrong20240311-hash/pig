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
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.vault.api.dto.*;
import com.pig4cloud.pig.vault.api.entity.Balance;
import com.pig4cloud.pig.vault.mapper.BalanceMapper;
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
@RequestMapping("/vault")
@Tag(description = "vault", name = "Vault Management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class VaultController {

	private final VaultFreezeService vaultFreezeService;

	private final BalanceMapper balanceMapper;

	/**
	 * Create a freeze (lock funds from available to frozen)
	 * @param request freeze request
	 * @return freeze response
	 */
	@PostMapping("/freeze/create")
	@Operation(summary = "Create Freeze", description = "Create a freeze to lock funds")
	@SysLog("Create Freeze")
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
	@SysLog("Release Freeze")
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
	@SysLog("Claim Freeze")
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
	@SysLog("Consume Freeze")
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
	 * @param assetId asset ID
	 * @return balance response
	 */
	@GetMapping("/balance")
	@Operation(summary = "Get Balance", description = "Get balance for an account and asset")
	public R<BalanceResponse> getBalance(@RequestParam("accountId") Long accountId,
			@RequestParam("assetId") Long assetId) {
		try {
			Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
				.eq(Balance::getAccountId, accountId)
				.eq(Balance::getAssetId, assetId));

			if (balance == null) {
				return R.failed("Balance not found");
			}

			BalanceResponse response = new BalanceResponse();
			response.setBalanceId(balance.getBalanceId());
			response.setAccountId(balance.getAccountId());
			response.setAssetId(balance.getAssetId());
			response.setAvailable(balance.getAvailable());
			response.setFrozen(balance.getFrozen());
			response.setUpdateTime(balance.getUpdateTime());

			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to get balance: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

}
