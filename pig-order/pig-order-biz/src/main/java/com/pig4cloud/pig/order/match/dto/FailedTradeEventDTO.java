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

package com.pig4cloud.pig.order.match.dto;

import com.pig4cloud.pig.order.api.dto.FillDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 失败的交易事件 DTO
 * <p>
 * 用于记录无法处理的交易事件,以便后续补偿重试
 * </p>
 *
 * @author lengleng
 * @date 2025/01/21
 */
@Data
@Schema(description = "失败的交易事件")
public class FailedTradeEventDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 匹配ID
	 */
	@Schema(description = "匹配ID")
	private String matchId;

	/**
	 * Taker订单ID
	 */
	@Schema(description = "Taker订单ID")
	private Long takerOrderId;

	/**
	 * 总成交量(原始long值)
	 */
	@Schema(description = "总成交量")
	private Long totalVolume;

	/**
	 * 时间戳
	 */
	@Schema(description = "时间戳")
	private Long timestamp;

	/**
	 * 成交明细列表
	 */
	@Schema(description = "成交明细列表")
	private List<FillDTO> fills;

	/**
	 * 幂等性键
	 */
	@Schema(description = "幂等性键")
	private String idempotencyKey;

}
