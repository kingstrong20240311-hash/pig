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
import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderCancel;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.match.MatchingEngineProperties;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

	private final MatchingEngineProperties matchingEngineProperties;

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

		Market market = marketService.getMarket(request.getMarketId());
		if (market == null) {
			throw new IllegalArgumentException("Market not found: " + request.getMarketId());
		}
		Integer symbolId = request.getOutcome() == Outcome.YES ? market.getSymbolIdYes() : market.getSymbolIdNo();
		if (symbolId == null) {
			throw new IllegalStateException("Market symbols not ready: " + request.getMarketId());
		}

		// 4. Generate order ID using configured workerId and datacenterId
		Long orderId = IdUtil.getSnowflake(nodeId, DATACENTER_ID).nextId();

		// 5. Create freeze in Vault
		CreateFreezeRequest freezeRequest = new CreateFreezeRequest();
		freezeRequest.setUserId(request.getUserId());
		// Determine freeze symbol based on side:
		// BUY orders: freeze USDC (default asset)
		// SELL orders: freeze the outcome token (YES or NO)
		String freezeSymbol = determineFreezeSymbol(request.getSide(), request.getMarketId(), request.getOutcome());
		freezeRequest.setSymbol(freezeSymbol);
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
		order.setOutcome(request.getOutcome());
		order.setSide(request.getSide());
		order.setOrderType(request.getType());
		order.setPrice(request.getPrice());
		order.setQuantity(request.getQuantity());
		order.setRemainingQuantity(request.getQuantity());
		OrderStatus previousStatus = order.getStatus();
		order.setStatus(OrderStatus.OPEN);
		log.info("Order status changed: orderId={}, {} -> {}, reason=init", orderId, previousStatus, order.getStatus());
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

		// 3.1 市价单不支持取消：直接拒绝，不发 ApiCancelOrder
		if (order.getOrderType() == OrderType.MARKET) {
			throw new IllegalArgumentException("MARKET_ORDER_CANCEL_NOT_SUPPORTED");
		}

		// 4. If order cannot be cancelled, return current status (no-op)
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
		OrderStatus previousStatus = order.getStatus();
		order.setStatus(OrderStatus.CANCEL_REQUESTED);
		log.info("Order status changed: orderId={}, {} -> {}, reason=cancel_requested", order.getOrderId(),
				previousStatus, order.getStatus());
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
		if (request.getOutcome() == null) {
			throw new IllegalArgumentException("Outcome is required");
		}

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
	 * Determine which asset symbol to freeze based on order side
	 * @param side order side (BUY/SELL)
	 * @param marketId market ID
	 * @param outcome outcome (YES/NO)
	 * @return symbol name to freeze
	 */
	private String determineFreezeSymbol(Side side, Long marketId, Outcome outcome) {
		if (side == Side.BUY) {
			// BUY orders: freeze USDC (default asset) to purchase outcome tokens
			return matchingEngineProperties.getAssetSymbol(matchingEngineProperties.getDefaultAsset());
		}
		else {
			// SELL orders: freeze outcome tokens (YES or NO) to sell for USDC
			// Symbol format matches MarketCreatedEventHandler: M{marketId}_{outcome}
			return buildOutcomeSymbol(marketId, outcome.name());
		}
	}

	/**
	 * Build outcome asset symbol (must match format in MarketCreatedEventHandler)
	 * @param marketId market ID
	 * @param outcome outcome name (YES/NO)
	 * @return symbol like "M1_YES" or "M1_NO"
	 */
	private String buildOutcomeSymbol(Long marketId, String outcome) {
		return "M" + marketId + "_" + outcome;
	}

	/**
	 * Calculate freeze amount based on order request
	 */
	private BigDecimal calculateFreezeAmount(CreateOrderRequest request) {
		// For BUY orders: freeze USDC amount = quantity * price
		// For SELL orders: freeze token quantity = quantity
		if (request.getSide() == Side.BUY) {
			// BUY: need to freeze USDC to purchase tokens
			if (request.getType() == OrderType.LIMIT) {
				return request.getQuantity().multiply(request.getPrice());
			}
			else {
				// MARKET order: freeze quantity * 1.0 (worst case price in prediction
				// markets)
				// Since max price is 1.0 in prediction markets, quantity equals the max
				// USDC needed
				return request.getQuantity();
			}
		}
		else {
			// SELL: need to freeze the outcome tokens (YES or NO)
			return request.getQuantity();
		}
	}

	/**
	 * Build CreateOrderResponse from Order
	 */
	private CreateOrderResponse buildCreateOrderResponse(Order order) {
		CreateOrderResponse response = new CreateOrderResponse();
		response.setOrderId(order.getOrderId() != null ? String.valueOf(order.getOrderId()) : null);
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
		response.setOrderId(orderId != null ? String.valueOf(orderId) : null);
		response.setStatus(OrderStatus.REJECTED);
		response.setRejectReason(reason);
		return response;
	}

	/**
	 * Build CancelOrderResponse from Order
	 */
	private CancelOrderResponse buildCancelOrderResponse(Order order) {
		CancelOrderResponse response = new CancelOrderResponse();
		response.setOrderId(order.getOrderId() != null ? String.valueOf(order.getOrderId()) : null);
		response.setStatus(order.getStatus());
		return response;
	}

	/**
	 * Publish OrderCreatedEvent via DomainEventPublisher
	 */
	private void publishOrderCreatedEvent(Order order) {
		OrderCreatedPayload payload = new OrderCreatedPayload(order.getOrderId(), order.getUserId(),
				order.getMarketId(), order.getOutcome() != null ? order.getOutcome().name() : null,
				order.getStatus().name());

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
