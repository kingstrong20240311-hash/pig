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
import java.time.Instant;

/**
 * Order Cancel Request Record
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Data
@Schema(description = "订单取消记录")
@TableName("ord_order_cancel")
@EqualsAndHashCode(callSuper = true)
public class OrderCancel extends Model<OrderCancel> {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Cancel ID
	 */
	@TableId
	@Schema(description = "取消ID")
	private Long cancelId;

	/**
	 * Order ID
	 */
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
	@Schema(description = "幂等键")
	private String idempotencyKey;

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
