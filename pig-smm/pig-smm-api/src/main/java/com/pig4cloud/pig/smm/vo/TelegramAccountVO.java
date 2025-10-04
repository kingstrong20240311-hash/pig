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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Telegram账号视图对象
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "Telegram账号视图对象")
public class TelegramAccountVO {

	/**
	 * 主键ID
	 */
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 账号昵称/显示名称
	 */
	@Schema(description = "账号昵称")
	private String nickName;

	/**
	 * 账号用户名
	 */
	@Schema(description = "账号用户名")
	private String username;

	/**
	 * 已加入的群组ID列表
	 */
	@Schema(description = "已加入的群组ID列表")
	private List<Long> groupIds;

	/**
	 * 是否可用
	 */
	@Schema(description = "是否可用")
	private Boolean isAvailable;

	/**
	 * 是否忙碌
	 */
	@Schema(description = "是否忙碌")
	private Boolean isBusy;

	/**
	 * 最后检查时间
	 */
	@Schema(description = "最后检查时间")
	private LocalDateTime lastCheckTime;

	/**
	 * 状态变更原因
	 */
	@Schema(description = "状态变更原因")
	private String statusReason;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

}