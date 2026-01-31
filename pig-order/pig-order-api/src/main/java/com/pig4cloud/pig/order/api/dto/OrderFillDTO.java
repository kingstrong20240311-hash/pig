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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Order Fill DTO
 *
 * @author lengleng
 * @date 2025/01/27
 */
@Data
@Schema(description = "订单成交明细DTO")
public class OrderFillDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Trade ID
	 */
	@Schema(description = "成交ID")
	private String tradeId;

	/**
	 * Match ID (for idempotency)
	 */
	@Schema(description = "撮合ID")
	private String matchId;

	/**
	 * Taker Order ID
	 */
	@Schema(description = "Taker订单ID")
	private String takerOrderId;

	/**
	 * Maker Order ID
	 */
	@Schema(description = "Maker订单ID")
	private String makerOrderId;

	/**
	 * Fill price
	 */
	@Schema(description = "成交价格")
	private BigDecimal price;

	/**
	 * Fill quantity
	 */
	@Schema(description = "成交数量")
	private BigDecimal quantity;

	/**
	 * Fee (optional)
	 */
	@Schema(description = "手续费")
	private BigDecimal fee;

	/**
	 * Create time - Unix timestamp in milliseconds
	 */
	@Schema(description = "创建时间")
	private Long createTime;

	/**
	 * Created by
	 */
	@Schema(description = "创建人")
	private String createBy;

}
