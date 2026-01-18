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

import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * Cancel Order Request DTO
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "取消订单请求")
public class CancelOrderRequest implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Order ID
	 */
	@NotNull(message = "订单ID不能为空")
	@Schema(description = "订单ID")
	private Long orderId;

	/**
	 * Cancel reason
	 */
	@Schema(description = "取消原因")
	private String reason;

	/**
	 * Idempotency key
	 */
	@NotNull(message = "幂等键不能为空")
	@Schema(description = "幂等键 (必填)")
	private String idempotencyKey;

}
