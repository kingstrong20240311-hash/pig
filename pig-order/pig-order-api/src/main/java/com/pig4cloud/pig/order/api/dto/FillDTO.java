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
import jakarta.validation.constraints.Positive;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fill DTO (used in MatchCommit)
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "成交明细")
public class FillDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Maker Order ID
	 */
	@NotNull(message = "Maker订单ID不能为空")
	@Schema(description = "Maker订单ID")
	private Long makerOrderId;

	/**
	 * Fill price
	 */
	@NotNull(message = "成交价格不能为空")
	@Positive(message = "成交价格必须大于0")
	@Schema(description = "成交价格")
	private BigDecimal price;

	/**
	 * Fill quantity
	 */
	@NotNull(message = "成交数量不能为空")
	@Positive(message = "成交数量必须大于0")
	@Schema(description = "成交数量")
	private BigDecimal quantity;

	/**
	 * Fee (optional)
	 */
	@Schema(description = "手续费")
	private BigDecimal fee;

}
