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
 * Market Status Enum
 *
 * @author lengleng
 * @date 2025/01/19
 */
@Getter
public enum MarketStatus {

	INACTIVE(0, "INACTIVE"),

	ACTIVE(1, "ACTIVE"),

	EXPIRED(2, "EXPIRED");

	@EnumValue
	@JsonValue
	private final int code;

	private final String description;

	MarketStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public static MarketStatus valueOf(int code) {
		for (MarketStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("Invalid MarketStatus code: " + code);
	}

	public boolean isActive() {
		return this == ACTIVE;
	}

}
