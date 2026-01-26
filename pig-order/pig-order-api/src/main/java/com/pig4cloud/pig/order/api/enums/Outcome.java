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
 * Market outcome enum.
 *
 * @author lengleng
 * @date 2026/01/25
 */
@Getter
public enum Outcome {

	YES(1, "YES"),

	NO(2, "NO");

	@EnumValue
	@JsonValue
	private final int code;

	private final String description;

	Outcome(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public static Outcome valueOf(int code) {
		for (Outcome outcome : values()) {
			if (outcome.code == code) {
				return outcome;
			}
		}
		throw new IllegalArgumentException("Invalid Outcome code: " + code);
	}

}
