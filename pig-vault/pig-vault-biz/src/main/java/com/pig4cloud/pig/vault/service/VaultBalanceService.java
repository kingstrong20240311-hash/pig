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

package com.pig4cloud.pig.vault.service;

import com.pig4cloud.pig.vault.api.dto.BalanceResponse;
import com.pig4cloud.pig.vault.api.dto.DepositRequest;

/**
 * Vault Balance Service
 *
 * @author luka
 * @date 2025-01-23
 */
public interface VaultBalanceService {

	/**
	 * Deposit funds to an account (increase available balance) Idempotent based on refId
	 * @param request deposit request
	 * @return balance response
	 */
	BalanceResponse deposit(DepositRequest request);

	/**
	 * Get balance for current logged-in user by asset symbol
	 * @param symbol asset symbol (e.g., USDC)
	 * @return balance response
	 */
	BalanceResponse getMyBalance(String symbol);

}
