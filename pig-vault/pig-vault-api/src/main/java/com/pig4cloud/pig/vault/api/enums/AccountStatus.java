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
 * Account Status Enum
 *
 * @author luka
 * @date 2025-01-14
 */
@Getter
@AllArgsConstructor
public enum AccountStatus {

	/**
	 * Active account - can perform all operations
	 */
	ACTIVE(1, "Active"),

	/**
	 * Frozen account - no new operations allowed
	 */
	FROZEN(2, "Frozen"),

	/**
	 * Closed account - permanently disabled
	 */
	CLOSED(3, "Closed");

	@EnumValue
	private final Integer code;

	private final String description;

	public static AccountStatus fromCode(Integer code) {
		for (AccountStatus status : AccountStatus.values()) {
			if (status.code.equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid AccountStatus code: " + code);
	}

}
