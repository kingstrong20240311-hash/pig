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
import java.util.Map;

/**
 * Commit Match Response DTO
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "撮合提交响应")
public class CommitMatchResponse implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Match ID
	 */
	@Schema(description = "撮合ID")
	private String matchId;

	/**
	 * Order states after match (orderId -> OrderStateDTO)
	 */
	@Schema(description = "订单状态映射 (orderId -> OrderStateDTO)")
	private Map<String, OrderStateDTO> orderStates;

	/**
	 * Whether settlement is required
	 */
	@Schema(description = "是否需要结算")
	private Boolean settlementRequired;

}
