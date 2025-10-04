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

package com.pig4cloud.pig.smm.vo;

import com.pig4cloud.pig.smm.entity.JoinTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 加群任务视图对象
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "加群任务视图对象")
public class JoinTaskVO {

	/**
	 * 主键ID
	 */
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 第三方平台任务ID
	 */
	@Schema(description = "第三方平台任务ID")
	private Long thirdPartyTaskId;

	/**
	 * 任务名称
	 */
	@Schema(description = "任务名称")
	private String taskName;

	/**
	 * 群组执行详情
	 */
	@Schema(description = "群组执行详情")
	private List<JoinTaskGroupVO> groups;

	/**
	 * 任务状态
	 */
	@Schema(description = "任务状态")
	private JoinTask.TaskStatusEnum taskStatus;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 开始执行时间
	 */
	@Schema(description = "开始执行时间")
	private LocalDateTime startTime;

	/**
	 * 完成时间
	 */
	@Schema(description = "完成时间")
	private LocalDateTime endTime;

	/**
	 * 创建人
	 */
	@Schema(description = "创建人")
	private String createdBy;

	/**
	 * 群组执行详情内部类
	 */
	@Data
	@Schema(description = "群组执行详情")
	public static class JoinTaskGroupVO {

		/**
		 * 群组信息
		 */
		@Schema(description = "群组信息")
		private TelegramGroupVO group;

		/**
		 * 执行状态
		 */
		@Schema(description = "执行状态")
		private String status;

	}

}
