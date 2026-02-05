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

import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Order DTO
 *
 * @author lengleng
 * @date 2025/01/24
 */
@Data
@Schema(description = "订单DTO")
public class OrderDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Order ID
	 */
	@Schema(description = "订单ID")
	private String orderId;

	/**
	 * User ID
	 */
	@Schema(description = "用户ID")
	private String userId;

	/**
	 * Market ID
	 */
	@Schema(description = "市场ID")
	private String marketId;

	/**
	 * Outcome: YES / NO
	 */
	@Schema(description = "Outcome: YES / NO")
	private Outcome outcome;

	/**
	 * Side: BUY / SELL
	 */
	@Schema(description = "方向: BUY / SELL")
	private Side side;

	/**
	 * Order Type: LIMIT / MARKET
	 */
	@Schema(description = "订单类型: LIMIT / MARKET")
	private OrderType orderType;

	/**
	 * Price (required for LIMIT orders)
	 */
	@Schema(description = "价格 (LIMIT订单必填)")
	private BigDecimal price;

	/**
	 * Original quantity
	 */
	@Schema(description = "原始数量")
	private BigDecimal quantity;

	/**
	 * Remaining quantity (matchable)
	 */
	@Schema(description = "剩余可成交数量")
	private BigDecimal remainingQuantity;

	/**
	 * Order status
	 */
	@Schema(description = "订单状态")
	private OrderStatus status;

	/**
	 * Time in force: GTC / IOC / FOK / GTD
	 */
	@Schema(description = "有效期: GTC / IOC / FOK / GTD")
	private TimeInForce timeInForce;

	/**
	 * Expiration time (for GTD orders) - Unix timestamp in milliseconds
	 */
	@Schema(description = "过期时间 (GTD订单使用)")
	private Long expireAt;

	/**
	 * Rejection reason
	 */
	@Schema(description = "拒绝原因")
	private String rejectReason;

	/**
	 * Idempotency key for deduplication
	 */
	@Schema(description = "幂等键")
	private String idempotencyKey;

	/**
	 * Version for optimistic locking
	 */
	@Schema(description = "乐观锁版本")
	private Integer version;

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

}
