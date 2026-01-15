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
 * Account Type Enum
 *
 * @author luka
 * @date 2025-01-14
 */
@Getter
@AllArgsConstructor
public enum AccountType {

	/**
	 * User asset account - real user funds for trading
	 */
	USER(1, "User"),

	/**
	 * System fee account - platform fee income
	 */
	SYSTEM_FEE(2, "System Fee"),

	/**
	 * System treasury account - platform owned funds for market making/liquidity
	 */
	SYSTEM_TREASURY(3, "System Treasury"),

	/**
	 * System insurance account - risk reserve for extreme cases
	 */
	SYSTEM_INSURANCE(4, "System Insurance");

	@EnumValue
	private final Integer code;

	private final String description;

	public static AccountType fromCode(Integer code) {
		for (AccountType type : AccountType.values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Invalid AccountType code: " + code);
	}

	/**
	 * Check if this account type allows freeze operations
	 */
	public boolean allowsFreeze() {
		return this == USER || this == SYSTEM_TREASURY;
	}

	/**
	 * Check if this account type is a system account
	 */
	public boolean isSystemAccount() {
		return this != USER;
	}

}
