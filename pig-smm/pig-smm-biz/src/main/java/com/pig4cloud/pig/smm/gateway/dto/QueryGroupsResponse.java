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
 * 查询全局群组响应
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "查询全局群组响应")
public class QueryGroupsResponse {

	/**
	 * 群组列表
	 */
	@Schema(description = "群组列表")
	private List<GroupInfo> groups;

	@Data
	@Schema(description = "群组信息")
	public static class GroupInfo {

		/**
		 * 群组记录ID
		 */
		@Schema(description = "群组记录ID")
		private Long id;

		/**
		 * 群组标题
		 */
		@Schema(description = "群组标题")
		private String title;

		/**
		 * 群组用户名
		 */
		@Schema(description = "群组用户名")
		private String username;

	}

}