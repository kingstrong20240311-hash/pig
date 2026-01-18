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

import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Create Order Request DTO
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * User ID
	 */
	@NotNull(message = "用户ID不能为空")
	@Schema(description = "用户ID")
	private Long userId;

	/**
	 * Market ID
	 */
	@NotNull(message = "市场ID不能为空")
	@Schema(description = "市场ID")
	private Long marketId;

	/**
	 * Side: BUY / SELL
	 */
	@NotNull(message = "订单方向不能为空")
	@Schema(description = "订单方向")
	private Side side;

	/**
	 * Order Type: LIMIT / MARKET
	 */
	@NotNull(message = "订单类型不能为空")
	@Schema(description = "订单类型")
	private OrderType type;

	/**
	 * Price (required for LIMIT orders)
	 */
	@Schema(description = "价格 (LIMIT订单必填)")
	private BigDecimal price;

	/**
	 * Quantity
	 */
	@NotNull(message = "数量不能为空")
	@Positive(message = "数量必须大于0")
	@Schema(description = "数量")
	private BigDecimal quantity;

	/**
	 * Time in force (default: GTC)
	 */
	@Schema(description = "有效期类型 (默认: GTC)")
	private TimeInForce timeInForce;

	/**
	 * Expiration time (for GTD orders)
	 */
	@Schema(description = "过期时间 (GTD订单使用)")
	private Instant expireAt;

	/**
	 * Idempotency key for deduplication
	 */
	@NotNull(message = "幂等键不能为空")
	@Schema(description = "幂等键 (必填)")
	private String idempotencyKey;

}
