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

package com.pig4cloud.pig.vault.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Ledger Entry Type Enum
 *
 * @author luka
 * @date 2025-01-14
 */
@Getter
@AllArgsConstructor
public enum LedgerType {

	/**
	 * Freeze operation (available -> frozen)
	 */
	FREEZE(1, "Freeze"),

	/**
	 * Unfreeze/Release operation (frozen -> available)
	 */
	UNFREEZE(2, "Unfreeze"),

	/**
	 * Claim freeze operation (mark freeze as claimed by settlement)
	 */
	CLAIM(3, "Claim"),

	/**
	 * Consume freeze operation (frozen -> spent, total decreases)
	 */
	CONSUME(4, "Consume"),

	/**
	 * Deposit operation (external -> available)
	 */
	DEPOSIT(5, "Deposit"),

	/**
	 * Withdraw operation (available -> external)
	 */
	WITHDRAW(6, "Withdraw"),

	/**
	 * Transfer in operation
	 */
	TRANSFER_IN(7, "Transfer In"),

	/**
	 * Transfer out operation
	 */
	TRANSFER_OUT(8, "Transfer Out"),

	/**
	 * Manual adjustment (increase)
	 */
	ADJUST_INCREASE(9, "Adjust Increase"),

	/**
	 * Manual adjustment (decrease)
	 */
	ADJUST_DECREASE(10, "Adjust Decrease"),

	/**
	 * Fee deduction
	 */
	FEE(11, "Fee"),

	/**
	 * Settlement credit
	 */
	SETTLEMENT_CREDIT(12, "Settlement Credit"),

	/**
	 * Settlement debit
	 */
	SETTLEMENT_DEBIT(13, "Settlement Debit");

	@EnumValue
	private final Integer code;

	private final String description;

	public static LedgerType fromCode(Integer code) {
		for (LedgerType type : LedgerType.values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid LedgerType code: " + code);
	}

}
