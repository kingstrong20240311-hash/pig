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

/**
 * Telegram群组信息实体类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("smm_telegram_group")
@Schema(description = "Telegram群组信息")
public class TelegramGroup extends BaseEntity {

	/**
	 * 主键ID
	 */
	@TableId(type = IdType.AUTO)
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 群组名称
	 */
	@Schema(description = "群组名称")
	private String groupName;

	/**
	 * Tg平台群唯一标识
	 */
	@Schema(description = "Tg平台群唯一标识")
	private String username;

	@Schema(description = "第三方账户ID")
	private Long thirdPartyAccountId;

	/*
	 * 暂时定为 0 永久不可发送 1 新加入可发送 2 持续可发送
	 */
	@Schema(description = "可发送分数")
	private Integer sendScore;

	private Boolean isJoinable;

	@Schema(description = "加入tg username")
	private String sendBy;

	/**
	 * 群组成员数量
	 */
	@Schema(description = "群组成员数量")
	private Integer memberCount;

}
