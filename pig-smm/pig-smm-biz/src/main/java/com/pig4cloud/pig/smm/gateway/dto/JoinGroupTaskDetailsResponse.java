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
 * 加群任务详情响应
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "加群任务详情响应")
public class JoinGroupTaskDetailsResponse {

	/**
	 * 页码
	 */
	@Schema(description = "页码")
	private Integer pageIndex;

	/**
	 * 页大小
	 */
	@Schema(description = "页大小")
	private Integer pageSize;

	/**
	 * 总记录数
	 */
	@Schema(description = "总记录数")
	private Integer totalRecords;

	/**
	 * 加群记录列表
	 */
	@Schema(description = "加群记录列表")
	private List<JoinGroupRecord> records;

	@Data
	@Schema(description = "加群记录")
	public static class JoinGroupRecord {

		/**
		 * 记录ID
		 */
		@Schema(description = "记录ID")
		private Long id;

		/**
		 * 任务ID
		 */
		@Schema(description = "任务ID")
		private Long taskId;

		/**
		 * TG群聊ID
		 */
		@Schema(description = "TG群聊ID")
		private Long tgChatId;

		/**
		 * TG群组标题
		 */
		@Schema(description = "TG群组标题")
		private String tgTitle;

		/**
		 * 加群状态
		 */
		@Schema(description = "加群状态")
		private Integer status;

		/**
		 * 加群结果
		 */
		@Schema(description = "加群结果")
		private String result;

		/**
		 * 创建时间
		 */
		@Schema(description = "创建时间")
		private String gmtCreated;

	}

}