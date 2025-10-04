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

package com.pig4cloud.pig.smm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 加群任务数据传输对象
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "加群任务数据传输对象")
public class JoinTaskDTO {

	/**
	 * 主键ID
	 */
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 第三方平台任务ID
	 */
	@Schema(description = "第三方平台任务ID")
	private String thirdPartyTaskId;

	/**
	 * 任务名称
	 */
	@NotBlank(message = "任务名称不能为空")
	@Schema(description = "任务名称")
	private String taskName;

	/**
	 * 目标群组ID列表
	 */
	@Schema(description = "目标群组ID列表")
	private List<Long> targetGroupIds;

}