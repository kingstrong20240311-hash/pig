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

package com.pig4cloud.pig.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.order.api.dto.*;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderCancel;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.mapper.OrderCancelMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.order.service.OrderService;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCancelRequestedPayload;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCreatedPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.vault.api.dto.CreateFreezeRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order Service Implementation
 *
 * @author lengleng
 * @date 2025/01/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderMapper orderMapper;

	private final OrderCancelMapper orderCancelMapper;

	private final DomainEventPublisher domainEventPublisher;

	private final VaultService vaultService;

	private final MarketService marketService;

	@Value("${node-id:0}")
	private long nodeId;

	private static final long DATACENTER_ID = 0L;

	private static final String DOMAIN_ORDER = "order";

	private static final String AGG_TYPE_ORDER = "Order";

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CreateOrderResponse createOrder(CreateOrderRequest request) {
		// 1. Validate idempotency key must be provided
		String idempotencyKey = request.getIdempotencyKey();
		if (StrUtil.isBlank(idempotencyKey)) {
			throw new IllegalArgumentException("Idempotency key is required");
		}

		// 2. Check idempotency - if order with this key exists, return existing
		Order existingOrder = orderMapper
			.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getIdempotencyKey, idempotencyKey).last("LIMIT 1"));

		if (existingOrder != null) {
			log.info("Order already exists for idempotency key: {}, orderId: {}", idempotencyKey,
					existingOrder.getOrderId());
			return buildCreateOrderResponse(existingOrder);
		}

		// 3. Validate request
		validateCreateOrderRequest(request);

		// 4. Generate order ID using configured workerId and datacenterId
		Long orderId = IdUtil.getSnowflake(nodeId, DATACENTER_ID).nextId();

		// 5. Create freeze in Vault
		CreateFreezeRequest freezeRequest = new CreateFreezeRequest();
		freezeRequest.setAccountId(request.getUserId());
		// TODO: get symbol from marketId mapping
		freezeRequest.setSymbol("USDC");
		freezeRequest.setAmount(calculateFreezeAmount(request));
		freezeRequest.setRefType(RefType.ORDER);
		freezeRequest.setRefId(String.valueOf(orderId));

		R<FreezeResponse> freezeResult = vaultService.createFreeze(freezeRequest);
		if (!freezeResult.isSuccess()) {
			log.error("Failed to create freeze for order: {}, reason: {}", orderId, freezeResult.getMsg());
			return buildRejectedResponse(orderId, "Insufficient balance or freeze failed");
		}

		// 6. Create order entity in OPEN status (not yet submitted to matching engine)
		Order order = new Order();
		order.setOrderId(orderId);
		order.setUserId(request.getUserId());
		order.setMarketId(request.getMarketId());
		order.setSide(request.getSide());
		order.setOrderType(request.getType());
		order.setPrice(request.getPrice());
		order.setQuantity(request.getQuantity());
		order.setRemainingQuantity(request.getQuantity());
		order.setStatus(OrderStatus.OPEN);
		order.setTimeInForce(request.getTimeInForce() != null ? request.getTimeInForce() : TimeInForce.GTC);
		// Convert Long timestamp (milliseconds) to Instant
		if (request.getExpireAt() != null) {
			order.setExpireAt(Instant.ofEpochMilli(request.getExpireAt()));
		}
		order.setIdempotencyKey(idempotencyKey);
		order.setVersion(0);

		orderMapper.insert(order);

		// 7. Emit OrderCreatedEvent to Outbox for asynchronous submission
		publishOrderCreatedEvent(order);

		log.info("Order created and persisted: orderId={}, marketId={}, side={}, price={}, quantity={}, status={}",
				orderId, order.getMarketId(), order.getSide(), order.getPrice(), order.getQuantity(),
				order.getStatus());

		return buildCreateOrderResponse(order);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CancelOrderResponse cancelOrder(CancelOrderRequest request) {
		// 1. Validate idempotency key must be provided
		String idempotencyKey = request.getIdempotencyKey();
		if (StrUtil.isBlank(idempotencyKey)) {
			throw new IllegalArgumentException("Idempotency key is required");
		}

		// 2. Check idempotency - if cancel record exists, return existing status
		OrderCancel existingCancel = orderCancelMapper.selectOne(
				Wrappers.<OrderCancel>lambdaQuery().eq(OrderCancel::getIdempotencyKey, idempotencyKey).last("LIMIT 1"));

		if (existingCancel != null) {
			log.info("Cancel request already exists for idempotency key: {}", idempotencyKey);
			Order order = orderMapper.selectById(existingCancel.getOrderId());
			return buildCancelOrderResponse(order);
		}

		// 3. Get order
		Order order = orderMapper.selectById(request.getOrderId());
		if (order == null) {
			throw new IllegalArgumentException("Order not found: " + request.getOrderId());
		}

		// 4. Check if order can be cancelled
		if (!order.isCancellable()) {
			log.warn("Order cannot be cancelled: orderId={}, status={}", order.getOrderId(), order.getStatus());
			return buildCancelOrderResponse(order);
		}

		// 5. Insert cancel record (idempotent anchor)
		OrderCancel orderCancel = new OrderCancel();
		orderCancel.setCancelId(IdUtil.getSnowflake(nodeId, DATACENTER_ID).nextId());
		orderCancel.setOrderId(request.getOrderId());
		orderCancel.setReason(request.getReason() != null ? request.getReason() : "User requested");
		orderCancel.setIdempotencyKey(idempotencyKey);
		orderCancelMapper.insert(orderCancel);

		// 6. Update order status to CANCEL_REQUESTED
		order.setStatus(OrderStatus.CANCEL_REQUESTED);
		orderMapper.updateById(order);

		// 7. Emit OrderCancelRequestedEvent for asynchronous processing
		publishOrderCancelRequestedEvent(order, idempotencyKey);

		log.info("Order cancel requested: orderId={}, reason={}, status={}", order.getOrderId(),
				orderCancel.getReason(), order.getStatus());

		return buildCancelOrderResponse(order);
	}

	/**
	 * Validate create order request
	 */
	private void validateCreateOrderRequest(CreateOrderRequest request) {
		marketService.assertMarketActive(request.getMarketId());

		// Validate LIMIT order must have price
		if (request.getType() == OrderType.LIMIT && request.getPrice() == null) {
			throw new IllegalArgumentException("LIMIT order must have price");
		}

		// Validate GTD order must have expireAt
		if (request.getTimeInForce() == TimeInForce.GTD && request.getExpireAt() == null) {
			throw new IllegalArgumentException("GTD order must have expireAt");
		}

		// Validate quantity > 0
		if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Quantity must be positive");
		}
	}

	/**
	 * Calculate freeze amount based on order request
	 */
	private BigDecimal calculateFreezeAmount(CreateOrderRequest request) {
		// For BUY orders: freeze = quantity * price
		// For SELL orders: freeze = quantity
		// TODO: adjust based on actual market requirements
		if (request.getType() == OrderType.LIMIT) {
			return request.getQuantity().multiply(request.getPrice());
		}
		else {
			// MARKET order: use estimated amount
			return request.getQuantity();
		}
	}

	/**
	 * Build CreateOrderResponse from Order
	 */
	private CreateOrderResponse buildCreateOrderResponse(Order order) {
		CreateOrderResponse response = new CreateOrderResponse();
		response.setOrderId(order.getOrderId());
		response.setStatus(order.getStatus());
		response.setRemainingQuantity(order.getRemainingQuantity());
		response.setRejectReason(order.getRejectReason());
		return response;
	}

	/**
	 * Build rejected response
	 */
	private CreateOrderResponse buildRejectedResponse(Long orderId, String reason) {
		CreateOrderResponse response = new CreateOrderResponse();
		response.setOrderId(orderId);
		response.setStatus(OrderStatus.REJECTED);
		response.setRejectReason(reason);
		return response;
	}

	/**
	 * Build CancelOrderResponse from Order
	 */
	private CancelOrderResponse buildCancelOrderResponse(Order order) {
		CancelOrderResponse response = new CancelOrderResponse();
		response.setOrderId(order.getOrderId());
		response.setStatus(order.getStatus());
		return response;
	}

	/**
	 * Publish OrderCreatedEvent via DomainEventPublisher
	 */
	private void publishOrderCreatedEvent(Order order) {
		OrderCreatedPayload payload = new OrderCreatedPayload(order.getOrderId(), order.getUserId(),
				order.getMarketId(), order.getStatus().name());

		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				"OrderCreated", // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	/**
	 * Publish OrderCancelRequestedEvent via DomainEventPublisher
	 */
	private void publishOrderCancelRequestedEvent(Order order, String idempotencyKey) {
		OrderCancelRequestedPayload payload = new OrderCancelRequestedPayload(order.getOrderId(), order.getUserId(),
				order.getMarketId(), order.getStatus().name(), idempotencyKey);

		DomainEventEnvelope<OrderCancelRequestedPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				"OrderCancelRequested", // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

}
