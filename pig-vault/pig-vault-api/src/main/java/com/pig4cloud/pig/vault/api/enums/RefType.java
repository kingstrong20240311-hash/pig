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
 * Reference Type Enum - what business entity this operation refers to
 *
 * @author luka
 * @date 2025-01-14
 */
@Getter
@AllArgsConstructor
public enum RefType {

	/**
	 * Order reference
	 */
	ORDER(1, "Order"),

	/**
	 * Settlement reference
	 */
	SETTLEMENT(2, "Settlement"),

	/**
	 * Deposit reference
	 */
	DEPOSIT(3, "Deposit"),

	/**
	 * Withdraw reference
	 */
	WITHDRAW(4, "Withdraw"),

	/**
	 * Transfer reference
	 */
	TRANSFER(5, "Transfer"),

	/**
	 * Manual adjustment reference
	 */
	ADJUSTMENT(6, "Adjustment"),

	/**
	 * System operation reference
	 */
	SYSTEM(7, "System");

	@EnumValue
	private final Integer code;

	private final String description;

	public static RefType fromCode(Integer code) {
		for (RefType type : RefType.values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid RefType code: " + code);
	}

}
