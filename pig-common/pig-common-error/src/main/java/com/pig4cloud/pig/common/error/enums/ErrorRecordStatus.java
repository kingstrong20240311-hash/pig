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

package com.pig4cloud.pig.common.error.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 错误记录状态枚举
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Getter
public enum ErrorRecordStatus {

	/**
	 * 新记录，待补偿
	 */
	NEW("NEW", "新记录"),

	/**
	 * 重试中
	 */
	RETRYING("RETRYING", "重试中"),

	/**
	 * 已解决
	 */
	RESOLVED("RESOLVED", "已解决"),

	/**
	 * 死信（超过最大重试次数）
	 */
	DEAD("DEAD", "死信");

	@EnumValue
	@JsonValue
	private final String value;

	private final String description;

	ErrorRecordStatus(String value, String description) {
		this.value = value;
		this.description = description;
	}

}
