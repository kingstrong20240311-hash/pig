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

package com.pig4cloud.pig.smm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 加群任务实体类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("join_task")
@Schema(description = "加群任务")
public class JoinTask extends BaseEntity {

	/**
	 * 主键ID
	 */
	@TableId(type = IdType.AUTO)
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 第三方平台任务ID
	 */
	@Schema(description = "第三方平台任务ID")
	private Long thirdPartyTaskId;

	/**
	 * 执行任务账号昵称
	 */
	@Schema(description = "执行任务账号昵称")
	private String accountNickname;

	/**
	 * 任务状态
	 */
	@Schema(description = "任务状态")
	private TaskStatusEnum status;

	@Schema(description = "tg账号")
	private Long tgAccountId;

	public enum TaskStatusEnum {
		READY(0), RUNNING(1), FINISH(3);

		@Getter
		private final Integer status;

		TaskStatusEnum(int status) {
			this.status = status;
		}

		public static TaskStatusEnum fromStatus(int status) {
			for (TaskStatusEnum e : TaskStatusEnum.values()) {
				if (e.getStatus().equals(status)) {
					return e;
				}
			}
			return TaskStatusEnum.FINISH;
		}
	}


}
