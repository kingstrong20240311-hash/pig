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

package com.pig4cloud.pig.smm.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 发布任务表
 * </p>
 *
 * @author lengleng
 * @since 2025-09-23
 */
@Data
@Schema(description = "发布任务")
public class SmmTask implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键id")
	private Long id;

	/**
	 * 任务名称
	 */
	@Schema(description = "任务名称")
	private String taskName;

	/**
	 * 关联消息ID
	 */
	@Schema(description = "关联消息ID")
	private Long messageId;

	/**
	 * 关联渠道ID
	 */
	@Schema(description = "关联渠道ID")
	private Long channelId;

	/**
	 * 关联账号ID
	 */
	@Schema(description = "关联账号ID")
	private Long accountId;

	/**
	 * Cron表达式
	 */
	@Schema(description = "Cron表达式")
	private String cron;

	/**
	 * 创建者
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建者")
	private String createBy;

	/**
	 * 更新者
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新者")
	private String updateBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	/**
	 * 删除标记
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
