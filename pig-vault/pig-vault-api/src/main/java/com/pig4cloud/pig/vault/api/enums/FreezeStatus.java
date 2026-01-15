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
 * Freeze Status Enum - lifecycle of a freeze
 *
 * @author luka
 * @date 2025-01-14
 */
@Getter
@AllArgsConstructor
public enum FreezeStatus {

	/**
	 * Freeze is held (active)
	 */
	HELD(1, "Held"),

	/**
	 * Freeze has been claimed by settlement
	 */
	CLAIMED(2, "Claimed"),

	/**
	 * Freeze has been released back to available
	 */
	RELEASED(3, "Released"),

	/**
	 * Freeze has been consumed (spent)
	 */
	CONSUMED(4, "Consumed"),

	/**
	 * Freeze was canceled (rollback/failed)
	 */
	CANCELED(5, "Canceled"),

	/**
	 * Freeze expired (TTL reached)
	 */
	EXPIRED(6, "Expired");

	@EnumValue
	private final Integer code;

	private final String description;

	public static FreezeStatus fromCode(Integer code) {
		for (FreezeStatus status : FreezeStatus.values()) {
			if (status.code.equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid FreezeStatus code: " + code);
	}

	/**
	 * Check if freeze is in a terminal state
	 */
	public boolean isTerminal() {
		return this == RELEASED || this == CONSUMED || this == CANCELED || this == EXPIRED;
	}

	/**
	 * Check if freeze is active (can be claimed/released/consumed)
	 */
	public boolean isActive() {
		return this == HELD;
	}

}
