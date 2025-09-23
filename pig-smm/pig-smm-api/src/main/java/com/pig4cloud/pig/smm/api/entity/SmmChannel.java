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
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 发布渠道表
 * </p>
 *
 * @author lengleng
 * @since 2025-09-23
 */
@Data
@Schema(description = "发布渠道")
public class SmmChannel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 渠道状态枚举
	 */
	@Getter
	public enum ChannelStatus {
		READABLE("可读"),
		WRITABLE("可写"),
		DISABLED("不可用");

		private final String description;

		ChannelStatus(String description) {
			this.description = description;
		}

	}

	/**
	 * 平台类型枚举
	 */
	@Getter
	public enum PlatformType {
		TELEGRAM("Telegram"),
		INSTAGRAM("Instagram"),
		TIKTOK("TikTok");

		private final String description;

		PlatformType(String description) {
			this.description = description;
		}
	}

	/**
	 * 主键ID
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "主键id")
	private Long id;

	/**
	 * 渠道名称
	 */
	@Schema(description = "渠道名称")
	private String channelName;

	/**
	 * 平台类型（TELEGRAM-Telegram，INSTAGRAM-Instagram，TIKTOK-TikTok）
	 */
	@Schema(description = "平台类型")
	private PlatformType platformType;

	/**
	 * 渠道状态（READABLE-可读，WRITABLE-可写，DISABLED-不可用）
	 */
	@Schema(description = "渠道状态")
	private ChannelStatus status;

	/**
	 * 渠道描述
	 */
	@Schema(description = "渠道描述")
	private String description;

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
