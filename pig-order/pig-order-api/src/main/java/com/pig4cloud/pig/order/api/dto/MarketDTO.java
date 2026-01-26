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

package com.pig4cloud.pig.order.api.dto;

import com.pig4cloud.pig.order.api.enums.MarketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Market DTO
 *
 * @author lengleng
 * @date 2025/01/24
 */
@Data
@Schema(description = "预测市场DTO")
public class MarketDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Market ID
	 */
	@Schema(description = "市场ID")
	private Long marketId;

	/**
	 * Market name
	 */
	@Schema(description = "市场名称")
	private String name;

	/**
	 * YES orderbook symbolId
	 */
	@Schema(description = "YES 订单簿 symbolId")
	private Integer symbolIdYes;

	/**
	 * NO orderbook symbolId
	 */
	@Schema(description = "NO 订单簿 symbolId")
	private Integer symbolIdNo;

	/**
	 * Market status
	 */
	@Schema(description = "市场状态")
	private MarketStatus status;

	/**
	 * Expiration time - Unix timestamp in milliseconds
	 */
	@Schema(description = "过期时间")
	private Long expireAt;

	/**
	 * Create time - Unix timestamp in milliseconds
	 */
	@Schema(description = "创建时间")
	private Long createTime;

	/**
	 * Update time - Unix timestamp in milliseconds
	 */
	@Schema(description = "更新时间")
	private Long updateTime;

	/**
	 * Delete flag
	 */
	@Schema(description = "删除标记")
	private String delFlag;

}
