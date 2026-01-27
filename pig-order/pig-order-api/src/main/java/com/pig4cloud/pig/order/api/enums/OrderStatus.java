/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pig4cloud.pig.order.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Order Status Enum
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Getter
public enum OrderStatus {

	/**
	 * Open - Order persisted but not yet submitted or submission not confirmed
	 */
	OPEN(1, "OPEN"),

	/**
	 * Matching - Accepted by matching engine
	 */
	MATCHING(2, "MATCHING"),

	/**
	 * Partially Filled - Partial execution, still matchable
	 */
	PARTIALLY_FILLED(3, "PARTIALLY_FILLED"),

	/**
	 * Filled - Fully executed
	 */
	FILLED(4, "FILLED"),

	/**
	 * Cancel Requested - Cancel request recorded, waiting for matching thread
	 */
	CANCEL_REQUESTED(5, "CANCEL_REQUESTED"),

	/**
	 * Cancelled - Final state
	 */
	CANCELLED(6, "CANCELLED"),

	/**
	 * Expired - Final state
	 */
	EXPIRED(7, "EXPIRED"),

	/**
	 * Rejected - Rejected by matching engine (business rejection)
	 */
	REJECTED(8, "REJECTED"),

	/**
	 * Failed - Submission failed (system/network/exception), Final state
	 */
	FAILED(9, "FAILED");

	@EnumValue
	private final int code;

	private final String description;

	OrderStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public static OrderStatus valueOf(int code) {
		for (OrderStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid OrderStatus code: " + code);
	}

	/**
	 * Check if this is a final state (terminal state)
	 */
	public boolean isFinalState() {
		return this == FILLED || this == CANCELLED || this == EXPIRED || this == REJECTED || this == FAILED;
	}

	/**
	 * Check if this status is matchable (can be in orderbook)
	 */
	public boolean isMatchable() {
		return this == OPEN || this == PARTIALLY_FILLED;
	}

}
