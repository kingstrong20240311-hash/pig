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

/**
 * 创建群发任务请求
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@Schema(description = "创建群发任务请求")
public class CreateSendTaskRequest {

	/**
	 * TG账号ID
	 */
	@Schema(description = "TG账号ID")
	private Long tgAccountId;

	/**
	 * 目标群组列表
	 */
	@Schema(description = "目标群组列表")
	private String targetGroupList;

	/**
	 * 发送文本
	 */
	@Schema(description = "发送文本")
	private String text;

	/**
	 * 时间单位
	 */
	@Schema(description = "时间单位")
	private String timeUnit;

	/**
	 * 发送间隔时间
	 */
	@Schema(description = "发送间隔时间")
	private Integer timeSpacing;

	/**
	 * 同时进行的发送数量限制
	 */
	@Schema(description = "同时进行的发送数量限制")
	private Integer limit;

}