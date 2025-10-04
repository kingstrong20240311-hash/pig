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

/**
 * Telegram群组视图对象
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "Telegram群组视图对象")
public class TelegramGroupVO {

	/**
	 * 主键ID
	 */
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * Telegram群组ID
	 */
	@Schema(description = "Telegram群组ID")
	private String groupId;

	/**
	 * 群组名称
	 */
	@Schema(description = "群组名称")
	private String groupName;

	/**
	 * 群组成员数量
	 */
	@Schema(description = "群组成员数量")
	private Integer memberCount;

	/**
	 * 加群失败次数
	 */
	@Schema(description = "加群失败次数")
	private Integer joinFailureCount;

	/**
	 * 总加群尝试次数
	 */
	@Schema(description = "总加群尝试次数")
	private Integer totalJoinCount;

	/**
	 * 发送失败次数
	 */
	@Schema(description = "发送失败次数")
	private Integer sendFailureCount;

	/**
	 * 总发送尝试次数
	 */
	@Schema(description = "总发送尝试次数")
	private Integer totalSendCount;

	/**
	 * 加群成功率
	 */
	@Schema(description = "加群成功率")
	private Double joinSuccessRate;

	/**
	 * 发送成功率
	 */
	@Schema(description = "发送成功率")
	private Double sendSuccessRate;

	/**
	 * 优先级分值
	 */
	@Schema(description = "优先级分值")
	private Double priorityScore;

	/**
	 * 最后加群尝试时间
	 */
	@Schema(description = "最后加群尝试时间")
	private LocalDateTime lastJoinAttempt;

	/**
	 * 最后发送尝试时间
	 */
	@Schema(description = "最后发送尝试时间")
	private LocalDateTime lastSendAttempt;

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