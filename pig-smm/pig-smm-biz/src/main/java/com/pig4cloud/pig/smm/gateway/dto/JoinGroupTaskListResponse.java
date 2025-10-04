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

import java.util.List;

/**
 * 加群任务列表响应
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "加群任务列表响应")
public class JoinGroupTaskListResponse {

	/**
	 * 任务列表
	 */
	@Schema(description = "任务列表")
	private List<JoinGroupTaskInfo> tasks;

	@Data
	@Schema(description = "加群任务信息")
	public static class JoinGroupTaskInfo {

		/**
		 * 任务ID
		 */
		@Schema(description = "任务ID")
		private Long taskId;

		/**
		 * 第三方任务ID
		 */
		@Schema(description = "第三方任务ID")
		private String thirdPartyTaskId;

		/**
		 * 任务名称
		 */
		@Schema(description = "任务名称")
		private String taskName;

		/**
		 * TG账号ID
		 */
		@Schema(description = "TG账号ID")
		private Long tgAccountId;

		/**
		 * 任务状态
		 */
		@Schema(description = "任务状态")
		private Integer taskStatus;

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

		/**
		 * 创建时间
		 */
		@Schema(description = "创建时间")
		private String gmtCreated;

	}

}