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

package com.pig4cloud.pig.common.error.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.error.enums.ErrorRecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 错误记录实体
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Data
@TableName("error_record")
@Schema(description = "错误记录")
public class ErrorRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	/**
	 * 业务唯一ID（ULID/UUID）
	 */
	@Schema(description = "业务唯一ID")
	private String errorId;

	/**
	 * 领域（order/vault/settlement等）
	 */
	@Schema(description = "领域")
	private String domain;

	/**
	 * 处理函数标识
	 */
	@Schema(description = "处理函数标识")
	private String handlerKey;

	/**
	 * 原始数据JSON
	 */
	@Schema(description = "原始数据JSON")
	private String payloadJson;

	/**
	 * 强类型反序列化类名
	 */
	@Schema(description = "强类型反序列化类名")
	private String payloadClass;

	/**
	 * 状态
	 */
	@Schema(description = "状态")
	private ErrorRecordStatus status;

	/**
	 * 尝试次数
	 */
	@Schema(description = "尝试次数")
	private Integer attempts;

	/**
	 * 下次重试时间
	 */
	@Schema(description = "下次重试时间")
	private Instant nextRetryTime;

	/**
	 * 错误摘要
	 */
	@Schema(description = "错误摘要")
	private String errorMessage;

	/**
	 * 堆栈信息
	 */
	@Schema(description = "堆栈信息")
	private String stackTrace;

	/**
	 * 最后一次错误时间
	 */
	@Schema(description = "最后一次错误时间")
	private Instant lastErrorAt;

	/**
	 * 扩展标签（JSON格式，用于检索）
	 */
	@Schema(description = "扩展标签")
	private String tags;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Instant createdAt;

	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Instant updatedAt;

}
