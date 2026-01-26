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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.order.api.dto.*;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderFill;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.match.OrderMatchService;
import com.pig4cloud.pig.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Order Controller
 *
 * @author lengleng
 * @date 2025/01/17
 */
@RestController
@RequiredArgsConstructor
@Tag(description = "order", name = "订单管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OrderController {

	private final OrderService orderService;

	private final OrderMatchService orderMatchService;

	private final OrderMapper orderMapper;

	private final OrderFillMapper orderFillMapper;

	/**
	 * Create a new order
	 * @param request create order request
	 * @return create order response
	 */
	@PostMapping("/create")
	@PreAuthorize("@pms.hasPermission('order_create')")
	@Operation(summary = "创建订单", description = "创建新订单并进行冻结")
	public R<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
		// 从安全上下文获取当前登录用户ID
		Long userId = SecurityUtils.getUser().getId();
		request.setUserId(userId);

		CreateOrderResponse response = orderService.createOrder(request);
		return R.ok(response);
	}

	/**
	 * Cancel an order
	 * @param request cancel order request
	 * @return cancel order response
	 */
	@PostMapping("/cancel")
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
	@PreAuthorize("@pms.hasPermission('order_match_commit')")
	@Operation(summary = "提交撮合结果", description = "撮合引擎提交成交明细")
	public R<CommitMatchResponse> commitMatch(@Valid @RequestBody CommitMatchRequest request) {
		CommitMatchResponse response = orderMatchService.commitMatch(request);
		return R.ok(response);
	}

	/**
	 * Get order by ID
	 * @param orderId order ID
	 * @return order details
	 */
	@GetMapping("/{orderId}")
	@Operation(summary = "查询订单", description = "根据订单ID查询订单详情")
	public R<OrderDTO> getOrder(@PathVariable("orderId") Long orderId) {
		Order order = orderMapper.selectById(orderId);
		if (order == null) {
			return R.failed("Order not found");
		}
		return R.ok(toOrderDTO(order));
	}

	/**
	 * Convert Order entity to OrderDTO
	 * @param order Order entity
	 * @return OrderDTO
	 */
	private OrderDTO toOrderDTO(Order order) {
		OrderDTO dto = new OrderDTO();
		dto.setOrderId(order.getOrderId());
		dto.setUserId(order.getUserId());
		dto.setMarketId(order.getMarketId());
		dto.setOutcome(order.getOutcome());
		dto.setSide(order.getSide());
		dto.setOrderType(order.getOrderType());
		dto.setPrice(order.getPrice());
		dto.setQuantity(order.getQuantity());
		dto.setRemainingQuantity(order.getRemainingQuantity());
		dto.setStatus(order.getStatus());
		dto.setTimeInForce(order.getTimeInForce());
		dto.setExpireAt(order.getExpireAt() != null ? order.getExpireAt().toEpochMilli() : null);
		dto.setRejectReason(order.getRejectReason());
		dto.setIdempotencyKey(order.getIdempotencyKey());
		dto.setVersion(order.getVersion());
		dto.setCreateTime(order.getCreateTime() != null ? order.getCreateTime().toEpochMilli() : null);
		dto.setUpdateTime(order.getUpdateTime() != null ? order.getUpdateTime().toEpochMilli() : null);
		return dto;
	}

	/**
	 * Get trades by order ID
	 * @param orderId order ID
	 * @return list of trades
	 */
	@GetMapping("/trades")
	@Operation(summary = "查询成交记录", description = "根据订单ID查询成交记录")
	public R<List<OrderFill>> getTrades(@RequestParam("orderId") Long orderId) {
		List<OrderFill> fills = orderFillMapper
			.selectList(Wrappers.<OrderFill>lambdaQuery()
				.eq(OrderFill::getTakerOrderId, orderId)
				.or()
				.eq(OrderFill::getMakerOrderId, orderId));
		return R.ok(fills);
	}

}
