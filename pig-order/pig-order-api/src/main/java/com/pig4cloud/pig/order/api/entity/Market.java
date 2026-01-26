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

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.pig4cloud.pig.order.api.enums.MarketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.Instant;

/**
 * Prediction Market Aggregate Root
 *
 * @author lengleng
 * @date 2025/01/19
 */
@Data
@Schema(description = "预测市场")
@TableName("ord_market")
@EqualsAndHashCode(callSuper = true)
public class Market extends Model<Market> {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId
	@Schema(description = "市场ID")
	private Long marketId;

	@Schema(description = "市场名称")
	private String name;

	@Schema(description = "YES 订单簿 symbolId")
	private Integer symbolIdYes;

	@Schema(description = "NO 订单簿 symbolId")
	private Integer symbolIdNo;

	@Schema(description = "市场状态")
	private MarketStatus status;

	@Schema(description = "过期时间")
	private Instant expireAt;

	@Schema(description = "创建时间")
	private Instant createTime;

	@Schema(description = "更新时间")
	private Instant updateTime;

	@Schema(description = "删除标记")
	private String delFlag;

}
