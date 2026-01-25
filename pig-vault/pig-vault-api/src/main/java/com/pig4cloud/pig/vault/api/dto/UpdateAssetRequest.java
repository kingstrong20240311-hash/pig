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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.io.Serializable;

/**
 * Update Asset Request
 *
 * @author luka
 * @date 2025-01-24
 */
@Data
@Schema(description = "Update Asset Request")
public class UpdateAssetRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Asset ID
	 */
	@NotNull(message = "Asset ID cannot be null")
	@Schema(description = "Asset ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long assetId;

	/**
	 * Decimal places
	 */
	@Min(value = 0, message = "Decimals must be >= 0")
	@Schema(description = "Decimal places", example = "6")
	private Integer decimals;

	/**
	 * Is active
	 */
	@Schema(description = "Is active", example = "true")
	private Boolean isActive;

}
