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
 * Ledger Direction Enum - accounting direction
 *
 * @author luka
 * @date 2025-01-14
 */
@Getter
@AllArgsConstructor
public enum Direction {

	/**
	 * Debit - decrease in asset (outflow)
	 */
	DEBIT(1, "Debit"),

	/**
	 * Credit - increase in asset (inflow)
	 */
	CREDIT(2, "Credit");

	@EnumValue
	private final Integer code;

	private final String description;

	public static Direction fromCode(Integer code) {
		for (Direction direction : Direction.values()) {
			if (direction.code.equals(code)) {
				return direction;
			}
		}
		throw new IllegalArgumentException("Invalid Direction code: " + code);
	}

}
