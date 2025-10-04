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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API健康状态实体类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Data
@TableName("api_health_status")
@Schema(description = "API健康状态")
public class ApiHealthStatus {

	/**
	 * 主键ID
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * API名称
	 */
	@Schema(description = "API名称")
	private String apiName;

	/**
	 * 是否可用
	 */
	@Schema(description = "是否可用")
	private Boolean isAvailable;

	/**
	 * 最后检查时间
	 */
	@Schema(description = "最后检查时间")
	private LocalDateTime lastCheckTime;

	/**
	 * 连续失败次数
	 */
	@Schema(description = "连续失败次数")
	private Integer consecutiveFailures;

	/**
	 * 响应时间（毫秒）
	 */
	@Schema(description = "响应时间（毫秒）")
	private Long responseTime;

	/**
	 * 错误信息
	 */
	@Schema(description = "错误信息")
	private String errorMessage;

}