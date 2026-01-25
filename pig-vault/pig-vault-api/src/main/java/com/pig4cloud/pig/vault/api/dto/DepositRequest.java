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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Deposit Request DTO
 *
 * @author luka
 * @date 2025-01-23
 */
@Data
@Schema(description = "Deposit Request")
public class DepositRequest {

	/**
	 * Account ID
	 */
	@NotNull(message = "Account ID cannot be null")
	@Schema(description = "Account ID")
	private Long accountId;

	/**
	 * Asset symbol (e.g., USDC)
	 */
	@NotBlank(message = "Symbol cannot be blank")
	@Schema(description = "Asset symbol", example = "USDC")
	private String symbol;

	/**
	 * Deposit amount (must be positive)
	 */
	@NotNull(message = "Amount cannot be null")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0")
	@Schema(description = "Deposit amount", example = "100.00")
	private BigDecimal amount;

	/**
	 * Reference ID (e.g., external transaction ID) for idempotency
	 */
	@NotBlank(message = "Reference ID cannot be blank")
	@Schema(description = "Reference ID for idempotency", example = "deposit-12345")
	private String refId;

}
