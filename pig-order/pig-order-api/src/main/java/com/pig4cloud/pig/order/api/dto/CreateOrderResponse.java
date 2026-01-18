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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Create Order Response DTO
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "创建订单响应")
public class CreateOrderResponse implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Order ID
	 */
	@Schema(description = "订单ID")
	private Long orderId;

	/**
	 * Order status
	 */
	@Schema(description = "订单状态")
	private OrderStatus status;

	/**
	 * Remaining quantity
	 */
	@Schema(description = "剩余可成交数量")
	private BigDecimal remainingQuantity;

	/**
	 * Reject reason (if status is REJECTED)
	 */
	@Schema(description = "拒绝原因")
	private String rejectReason;

}
