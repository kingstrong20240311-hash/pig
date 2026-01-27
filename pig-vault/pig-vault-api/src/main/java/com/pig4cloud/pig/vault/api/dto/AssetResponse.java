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

import java.io.Serializable;

/**
 * Asset Response
 *
 * @author luka
 * @date 2025-01-24
 */
@Data
@Schema(description = "Asset Response")
public class AssetResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Asset ID
	 */
	@Schema(description = "Asset ID")
	private Long assetId;

	/**
	 * Asset symbol (e.g., USDC, ETH)
	 */
	@Schema(description = "Asset symbol")
	private String symbol;

	/**
	 * Currency ID for exchange-core
	 */
	@Schema(description = "Currency ID")
	private Integer currencyId;

	/**
	 * Decimal places
	 */
	@Schema(description = "Decimal places")
	private Integer decimals;

	/**
	 * Is active
	 */
	@Schema(description = "Is active")
	private Boolean isActive;

	/**
	 * Create time
	 */
	@Schema(description = "Create time")
	private Long createTime; // Unix timestamp in milliseconds

}
