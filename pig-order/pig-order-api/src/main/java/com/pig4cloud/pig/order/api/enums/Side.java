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
 * Order Side Enum
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Getter
public enum Side {

	/**
	 * Buy
	 */
	BUY(1, "BUY"),

	/**
	 * Sell
	 */
	SELL(2, "SELL");

	@EnumValue
	@JsonValue
	private final int code;

	private final String description;

	Side(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public static Side valueOf(int code) {
		for (Side side : values()) {
			if (side.code == code) {
				return side;
			}
		}
		throw new IllegalArgumentException("Invalid Side code: " + code);
	}

}
