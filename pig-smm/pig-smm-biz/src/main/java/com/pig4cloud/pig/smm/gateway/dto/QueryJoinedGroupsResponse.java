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
 * 查询已加入群组响应
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "查询已加入群组响应")
public class QueryJoinedGroupsResponse {

	/**
	 * 已加入群组列表
	 */
	@Schema(description = "已加入群组列表")
	private List<JoinedGroupInfo> groups;

	@Data
	@Schema(description = "已加入群组信息")
	public static class JoinedGroupInfo {

		/**
		 * 群组记录ID
		 */
		@Schema(description = "群组记录ID")
		private Long id;

		/**
		 * Telegram群组ID
		 */
		@Schema(description = "Telegram群组ID")
		private String tgId;

		/**
		 * 群组名称
		 */
		@Schema(description = "群组名称")
		private String title;

		/**
		 * 群组用户名
		 */
		@Schema(description = "群组用户名")
		private String username;

		/**
		 * 成员数量
		 */
		@Schema(description = "成员数量")
		private Integer memberCount;

		/**
		 * 是否可发送消息
		 */
		@Schema(description = "是否可发送消息")
		private Boolean canSendMessages;

		/**
		 * 是否可邀请用户
		 */
		@Schema(description = "是否可邀请用户")
		private Boolean canInviteUsers;

	}

}