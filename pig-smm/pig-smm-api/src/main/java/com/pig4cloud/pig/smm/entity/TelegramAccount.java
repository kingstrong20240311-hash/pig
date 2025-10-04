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
 * Telegram账号信息实体类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("smm_telegram_account")
@Schema(description = "Telegram账号信息")
public class TelegramAccount extends BaseEntity {

	/**
	 * 主键ID
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 第三方账户ID
	 */
	@Schema(description = "第三方账户ID")
	private Long thirdPartyAccountId;

	/**
	 * 账号昵称（firstName + lastName）
	 */
	@Schema(description = "账号昵称")
	private String nickName;

	/**
	 * TG平台用户名（唯一标识）
	 */
	@Schema(description = "TG平台用户名")
	private String username;

	/**
	 * 手机号
	 */
	@Schema(description = "手机号")
	private String phone;

	/**
	 * TG ID
	 */
	@Schema(description = "TG ID")
	private String tgId;

	/**
	 * 已加入的群组ID列表，JSON格式，如：[1,2,3,4,5]
	 */
	@Schema(description = "已加入的群组ID列表")
	private String groups;

	/**
	 * 是否可用
	 */
	@Schema(description = "是否可用")
	private Boolean isAvailable;

	/**
	 * 是否忙碌
	 */
	@Schema(description = "是否忙碌")
	private Boolean isJoining;

	private Long joinTaskId;

	private Boolean isSending;

	private Long sendTaskId;

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

}
