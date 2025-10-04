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

package com.pig4cloud.pig.smm.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 查询账户列表响应
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "查询账户列表响应")
public class QueryAccountsResponse {

	/**
	 * 账户列表
	 */
	@Schema(description = "账户列表")
	private List<AccountInfo> accounts;

	@Data
	@Schema(description = "账户信息")
	public static class AccountInfo {

		/**
		 * 账户ID
		 */
		@Schema(description = "账户ID")
		private Long id;

		/**
		 * UUID
		 */
		@Schema(description = "UUID")
		private String uuid;

		/**
		 * 用户ID
		 */
		@Schema(description = "用户ID")
		private Long userId;

		/**
		 * 账户组ID
		 */
		@Schema(description = "账户组ID")
		private Long tgAccountGroupId;

		/**
		 * 账户组标题
		 */
		@Schema(description = "账户组标题")
		private String tgAccountGroupTitle;

		/**
		 * 备注
		 */
		@Schema(description = "备注")
		private String remark;

		/**
		 * 手机号
		 */
		@Schema(description = "手机号")
		private String phone;

		/**
		 * 地区代码
		 */
		@Schema(description = "地区代码")
		private String region;

		/**
		 * TG ID
		 */
		@Schema(description = "TG ID")
		private String tgId;

		/**
		 * 名字
		 */
		@Schema(description = "名字")
		private String firstName;

		/**
		 * 姓氏
		 */
		@Schema(description = "姓氏")
		private String lastName;

		/**
		 * 用户名
		 */
		@Schema(description = "用户名")
		private String username;

		/**
		 * 是否为高级用户
		 */
		@Schema(description = "是否为高级用户")
		private Boolean isPremium;

		/**
		 * 状态
		 */
		@Schema(description = "状态")
		private Boolean status;

		/**
		 * 是否冻结
		 */
		@Schema(description = "是否冻结")
		private Boolean isFrozen;

		/**
		 * 用户数量
		 */
		@Schema(description = "用户数量")
		private Integer userCount;

		/**
		 * 群组数量
		 */
		@Schema(description = "群组数量")
		private Integer groupCount;

		/**
		 * 频道数量
		 */
		@Schema(description = "频道数量")
		private Integer channelCount;

		/**
		 * 聊天数量
		 */
		@Schema(description = "聊天数量")
		private Integer chatCount;

		/**
		 * 默认回复
		 */
		@Schema(description = "默认回复")
		private String defaultReply;

		/**
		 * 创建时间
		 */
		@Schema(description = "创建时间")
		private String gmtCreated;

		/**
		 * 修改时间
		 */
		@Schema(description = "修改时间")
		private String gmtModified;

		/**
		 * 当前任务标题
		 */
		@Schema(description = "当前任务标题")
		private String taskTitle;

		/**
		 * 当前任务ID
		 */
		@Schema(description = "当前任务ID")
		private Long taskId;

		/**
		 * 任务状态
		 */
		@Schema(description = "任务状态")
		private Integer taskStatus;

	}

}