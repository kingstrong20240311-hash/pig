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

package com.pig4cloud.pig.order.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.order.api.dto.*;
import com.pig4cloud.pig.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Order Controller
 *
 * @author lengleng
 * @date 2025/01/17
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
@Tag(description = "order", name = "订单管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OrderController {

	private final OrderService orderService;

	/**
	 * Create a new order
	 * @param request create order request
	 * @return create order response
	 */
	@PostMapping("/create")
	@SysLog("创建订单")
	@PreAuthorize("@pms.hasPermission('order_create')")
	@Operation(summary = "创建订单", description = "创建新订单并进行冻结")
	public R<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
		CreateOrderResponse response = orderService.createOrder(request);
		return R.ok(response);
	}

	/**
	 * Cancel an order
	 * @param request cancel order request
	 * @return cancel order response
	 */
	@PostMapping("/cancel")
	@SysLog("取消订单")
	@PreAuthorize("@pms.hasPermission('order_cancel')")
	@Operation(summary = "取消订单", description = "请求取消订单")
	public R<CancelOrderResponse> cancelOrder(@Valid @RequestBody CancelOrderRequest request) {
		CancelOrderResponse response = orderService.cancelOrder(request);
		return R.ok(response);
	}

	/**
	 * Commit match results (called by matching engine)
	 * @param request commit match request
	 * @return commit match response
	 */
	@PostMapping("/commit-match")
	@SysLog("提交撮合结果")
	@PreAuthorize("@pms.hasPermission('order_match_commit')")
	@Operation(summary = "提交撮合结果", description = "撮合引擎提交成交明细")
	public R<CommitMatchResponse> commitMatch(@Valid @RequestBody CommitMatchRequest request) {
		CommitMatchResponse response = orderService.commitMatch(request);
		return R.ok(response);
	}

}
