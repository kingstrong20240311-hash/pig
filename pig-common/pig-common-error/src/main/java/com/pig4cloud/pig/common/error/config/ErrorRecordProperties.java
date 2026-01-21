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

package com.pig4cloud.pig.common.error.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 错误记录配置属性
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Data
@ConfigurationProperties(prefix = "pig.error")
public class ErrorRecordProperties {

	/**
	 * 最大重试次数
	 */
	private int maxAttempts = 5;

	/**
	 * 重试延迟（秒）
	 */
	private long retryDelaySeconds = 60;

	/**
	 * 堆栈信息最大长度
	 */
	private int stackTraceMaxLength = 4000;

	/**
	 * 是否使用指数退避策略
	 */
	private boolean useExponentialBackoff = true;

	/**
	 * 指数退避基数
	 */
	private double exponentialBackoffMultiplier = 2.0;

}
