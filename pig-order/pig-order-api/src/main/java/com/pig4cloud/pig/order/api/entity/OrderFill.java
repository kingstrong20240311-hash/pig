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

package com.pig4cloud.pig.order.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order Fill (Trade Detail)
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "订单成交明细")
@TableName("ord_order_fill")
@EqualsAndHashCode(callSuper = true)
public class OrderFill extends Model<OrderFill> {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Trade ID
	 */
	@TableId
	@Schema(description = "成交ID")
	private Long tradeId;

	/**
	 * Match ID (for idempotency)
	 */
	@Schema(description = "撮合ID")
	private String matchId;

	/**
	 * Taker Order ID
	 */
	@Schema(description = "Taker订单ID")
	private Long takerOrderId;

	/**
	 * Maker Order ID
	 */
	@Schema(description = "Maker订单ID")
	private Long makerOrderId;

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
	 * Create time
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private Instant createTime;

	/**
	 * Created by
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

}
