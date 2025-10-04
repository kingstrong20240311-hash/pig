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

/**
 * Telegram账号数据传输对象
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "Telegram账号数据传输对象")
public class TelegramAccountDTO {

	/**
	 * 主键ID
	 */
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 账号昵称/显示名称
	 */
	@NotBlank(message = "账号昵称不能为空")
	@Schema(description = "账号昵称")
	private String nickName;

	/**
	 * 账号用户名
	 */
	@NotBlank(message = "账号用户名不能为空")
	@Schema(description = "账号用户名")
	private String username;

	/**
	 * 是否可用
	 */
	@Schema(description = "是否可用")
	private Boolean isAvailable;

	/**
	 * 状态变更原因
	 */
	@Schema(description = "状态变更原因")
	private String statusReason;

}