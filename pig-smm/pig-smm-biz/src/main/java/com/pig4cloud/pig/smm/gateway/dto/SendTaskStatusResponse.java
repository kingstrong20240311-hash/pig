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

package com.pig4cloud.pig.smm.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送任务状态响应
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "发送任务状态响应")
public class SendTaskStatusResponse {

	/**
	 * 任务ID
	 */
	@Schema(description = "任务ID")
	private String taskId;

	/**
	 * 任务状态
	 */
	@Schema(description = "任务状态")
	private String status;

	/**
	 * 成功数量
	 */
	@Schema(description = "成功数量")
	private Integer successCount;

	/**
	 * 失败数量
	 */
	@Schema(description = "失败数量")
	private Integer failureCount;

}