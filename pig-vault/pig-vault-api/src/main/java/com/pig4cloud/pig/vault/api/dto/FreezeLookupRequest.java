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

import com.pig4cloud.pig.vault.api.enums.RefType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * Freeze Lookup Request DTO - for Release/Claim/Consume operations
 *
 * @author luka
 * @date 2025-01-14
 */
@Data
@Schema(description = "Freeze Lookup Request")
public class FreezeLookupRequest {

	/**
	 * Reference type (ORDER, SETTLEMENT, etc.)
	 */
	@NotNull(message = "Reference type cannot be null")
	@Schema(description = "Reference type", requiredMode = Schema.RequiredMode.REQUIRED)
	private RefType refType;

	/**
	 * Reference ID
	 */
	@NotNull(message = "Reference ID cannot be null")
	@Schema(description = "Reference ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String refId;

}
