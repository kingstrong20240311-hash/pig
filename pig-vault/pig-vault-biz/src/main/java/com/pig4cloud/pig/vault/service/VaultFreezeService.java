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

import com.pig4cloud.pig.vault.api.dto.CreateFreezeRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;

/**
 * Vault Freeze Service
 *
 * @author luka
 * @date 2025-01-14
 */
public interface VaultFreezeService {

	/**
	 * Create a freeze (lock funds from available to frozen)
	 * Idempotent based on refType + refId
	 *
	 * @param request freeze request
	 * @return freeze response
	 */
	FreezeResponse createFreeze(CreateFreezeRequest request);

	/**
	 * Release a freeze (unlock funds from frozen back to available)
	 * Idempotent based on refType + refId
	 *
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	FreezeResponse releaseFreeze(FreezeLookupRequest request);

	/**
	 * Claim a freeze (mark freeze as claimed by settlement process)
	 * Idempotent based on refType + refId
	 *
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	FreezeResponse claimFreeze(FreezeLookupRequest request);

	/**
	 * Consume a freeze (spend frozen funds, total decreases)
	 * Idempotent based on refType + refId
	 *
	 * @param request freeze lookup request
	 * @return freeze response
	 */
	FreezeResponse consumeFreeze(FreezeLookupRequest request);

}
