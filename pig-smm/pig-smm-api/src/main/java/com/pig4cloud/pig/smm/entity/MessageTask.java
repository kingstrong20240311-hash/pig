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

import java.time.LocalDateTime;

/**
 * 消息任务实体类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_task")
@Schema(description = "消息任务")
public class MessageTask extends BaseEntity {

	/**
	 * 主键ID
	 */
	@TableId(type = IdType.ASSIGN_ID)
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
	@Schema(description = "任务名称")
	private String taskName;

	/**
	 * 消息内容
	 */
	@Schema(description = "消息内容")
	private String messageContent;

	/**
	 * 执行账号ID
	 */
	@Schema(description = "执行账号ID")
	private Long accountId;

	/**
	 * 目标群组ID列表，JSON格式
	 */
	@Schema(description = "目标群组ID列表")
	private String targetGroups;

	/**
	 * 任务状态
	 */
	@Schema(description = "任务状态")
	private String taskStatus;

	/**
	 * 成功发送群组数量
	 */
	@Schema(description = "成功发送群组数量")
	private Integer successCount;

	/**
	 * 失败发送群组数量
	 */
	@Schema(description = "失败发送群组数量")
	private Integer failureCount;

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

}