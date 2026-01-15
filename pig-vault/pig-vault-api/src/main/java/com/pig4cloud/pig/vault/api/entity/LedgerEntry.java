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
import com.pig4cloud.pig.vault.api.enums.Direction;
import com.pig4cloud.pig.vault.api.enums.LedgerType;
import com.pig4cloud.pig.vault.api.enums.RefType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ledger Entry Entity - Immutable audit log
 *
 * @author luka
 * @date 2025-01-14
 */
@Data
@TableName("vault_ledger_entry")
@Schema(description = "Ledger Entry")
public class LedgerEntry implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Entry ID
	 */
	@TableId(value = "entry_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Entry ID")
	private Long entryId;

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
	 * Ledger entry type
	 */
	@Schema(description = "Ledger entry type")
	private LedgerType entryType;

	/**
	 * Direction (DEBIT/CREDIT)
	 */
	@Schema(description = "Direction")
	private Direction direction;

	/**
	 * Amount
	 */
	@Schema(description = "Amount")
	private BigDecimal amount;

	/**
	 * Idempotency key for duplicate detection
	 */
	@Schema(description = "Idempotency key")
	private String idempotencyKey;

	/**
	 * Reference type
	 */
	@Schema(description = "Reference type")
	private RefType refType;

	/**
	 * Reference ID
	 */
	@Schema(description = "Reference ID")
	private String refId;

	/**
	 * Available balance before operation
	 */
	@Schema(description = "Available balance before")
	private BigDecimal beforeAvailable;

	/**
	 * Frozen balance before operation
	 */
	@Schema(description = "Frozen balance before")
	private BigDecimal beforeFrozen;

	/**
	 * Available balance after operation
	 */
	@Schema(description = "Available balance after")
	private BigDecimal afterAvailable;

	/**
	 * Frozen balance after operation
	 */
	@Schema(description = "Frozen balance after")
	private BigDecimal afterFrozen;

	/**
	 * Create time (immutable)
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "Create time")
	private Instant createTime;

}
