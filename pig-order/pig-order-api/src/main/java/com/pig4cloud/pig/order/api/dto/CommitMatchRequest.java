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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Commit Match Request DTO (from matching engine)
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "撮合提交请求")
public class CommitMatchRequest implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Match ID (idempotency key)
	 */
	@NotNull(message = "撮合ID不能为空")
	@Schema(description = "撮合ID (幂等主键)")
	private String matchId;

	/**
	 * Taker Order ID
	 */
	@NotNull(message = "Taker订单ID不能为空")
	@Schema(description = "Taker订单ID")
	private Long takerOrderId;

	/**
	 * Maker fills (one taker can match multiple makers)
	 */
	@NotEmpty(message = "成交明细不能为空")
	@Valid
	@Schema(description = "Maker成交明细列表")
	private List<FillDTO> fills;

	/**
	 * Idempotency key
	 */
	@NotNull(message = "幂等键不能为空")
	@Schema(description = "幂等键 (必填)")
	private String idempotencyKey;

}
