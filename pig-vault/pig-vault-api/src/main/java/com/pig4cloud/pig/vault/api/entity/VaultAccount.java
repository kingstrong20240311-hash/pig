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
import com.pig4cloud.pig.vault.api.enums.AccountStatus;
import com.pig4cloud.pig.vault.api.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * Vault Account Entity
 *
 * @author luka
 * @date 2025-01-14
 */
@Data
@TableName("vault_account")
@Schema(description = "Vault Account")
public class VaultAccount implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Account ID
	 */
	@TableId(value = "account_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Account ID")
	private Long accountId;

	/**
	 * User ID (nullable for system accounts)
	 */
	@Schema(description = "User ID")
	private Long userId;

	/**
	 * Account Type
	 */
	@Schema(description = "Account Type")
	private AccountType accountType;

	/**
	 * Account Status
	 */
	@Schema(description = "Account Status")
	private AccountStatus status;

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

}
