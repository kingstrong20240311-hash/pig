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

package com.pig4cloud.pig.vault.api.dto;

import com.pig4cloud.pig.vault.api.enums.FreezeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Freeze Response DTO
 *
 * @author luka
 * @date 2025-01-14
 */
@Data
@Schema(description = "Freeze Response")
public class FreezeResponse {

	/**
	 * Freeze ID
	 */
	@Schema(description = "Freeze ID")
	private Long freezeId;

	/**
	 * Freeze status
	 */
	@Schema(description = "Freeze status")
	private FreezeStatus status;

	/**
	 * Freeze amount
	 */
	@Schema(description = "Freeze amount")
	private BigDecimal amount;

	/**
	 * Claim time (only meaningful for create/claim operations)
	 */
	@Schema(description = "Claim time")
	private Instant claimTime;

}
