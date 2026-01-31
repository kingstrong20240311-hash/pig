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

package com.pig4cloud.pig.vault.api.feign;

import com.pig4cloud.pig.common.core.constant.ServiceNameConstants;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.feign.annotation.NoToken;
import com.pig4cloud.pig.vault.api.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Vault Service Feign Interface
 *
 * @author luka
 * @date 2025-01-14
 */
@FeignClient(contextId = "vaultService", value = ServiceNameConstants.VAULT_SERVICE)
public interface VaultService {

	/**
	 * Create a freeze (lock funds from available to frozen)
	 * @param request freeze request
	 * @return freeze response
	 */
	@PostMapping("/freeze/create")
	R<FreezeResponse> createFreeze(@Valid @RequestBody CreateFreezeRequest request);

	/**
	 * Release a freeze (unlock funds from frozen back to available)
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	@NoToken
	@PostMapping("/freeze/release")
	R<FreezeResponse> releaseFreeze(@Valid @RequestBody FreezeLookupRequest request);

	/**
	 * Claim a freeze (mark freeze as claimed by settlement process)
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	@PostMapping("/freeze/claim")
	R<FreezeResponse> claimFreeze(@Valid @RequestBody FreezeLookupRequest request);

	/**
	 * Consume a freeze (spend frozen funds, total decreases)
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	@PostMapping("/freeze/consume")
	R<FreezeResponse> consumeFreeze(@Valid @RequestBody FreezeLookupRequest request);

	/**
	 * Get balance for an account and asset
	 * @param accountId account ID
	 * @param symbol asset symbol
	 * @return balance response
	 */
	@GetMapping("/balance")
	R<BalanceResponse> getBalance(@RequestParam("accountId") Long accountId, @RequestParam("symbol") String symbol);

	/**
	 * Deposit funds to an account (increase available balance)
	 * @param request deposit request
	 * @return balance response
	 */
	@PostMapping("/deposit")
	R<BalanceResponse> deposit(@Valid @RequestBody DepositRequest request);

}
