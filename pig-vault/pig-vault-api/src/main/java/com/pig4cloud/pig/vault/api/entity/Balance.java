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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Balance Entity
 *
 * @author luka
 * @date 2025-01-14
 */
@Data
@TableName("vault_balance")
@Schema(description = "Balance")
public class Balance implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Balance ID
	 */
	@TableId(value = "balance_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Balance ID")
	private Long balanceId;

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
	 * Available balance
	 */
	@Schema(description = "Available balance")
	private BigDecimal available;

	/**
	 * Frozen balance
	 */
	@Schema(description = "Frozen balance")
	private BigDecimal frozen;

	/**
	 * Version for optimistic locking
	 */
	@Version
	@Schema(description = "Version")
	private Long version;

	/**
	 * Update time
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "Update time")
	private Instant updateTime;

}
