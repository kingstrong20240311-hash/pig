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

package com.pig4cloud.pig.vault.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.vault.api.enums.FreezeStatus;
import com.pig4cloud.pig.vault.api.enums.RefType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Freeze Entity
 *
 * @author luka
 * @date 2025-01-14
 */
@Data
@TableName("vault_freeze")
@Schema(description = "Freeze")
public class Freeze implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Freeze ID
	 */
	@TableId(value = "freeze_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Freeze ID")
	private Long freezeId;

	/**
	 * Account ID
	 */
	@Schema(description = "Account ID")
	private Long accountId;

	/**
	 * Asset ID
	 */
	@Schema(description = "Asset ID")
	private Long assetId;

	/**
	 * Freeze amount
	 */
	@Schema(description = "Freeze amount")
	private BigDecimal amount;

	/**
	 * Freeze status
	 */
	@Schema(description = "Freeze status")
	private FreezeStatus status;

	/**
	 * Reference type (ORDER, SETTLEMENT, etc.)
	 */
	@Schema(description = "Reference type")
	private RefType refType;

	/**
	 * Reference ID
	 */
	@Schema(description = "Reference ID")
	private String refId;

	/**
	 * Version for optimistic locking
	 */
	@Version
	@Schema(description = "Version")
	private Long version;

	/**
	 * Create time
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "Create time")
	private Instant createTime;

	/**
	 * Update time
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "Update time")
	private Instant updateTime;

	/**
	 * Claim time (when freeze was claimed by settlement)
	 */
	@Schema(description = "Claim time")
	private Instant claimTime;

}
