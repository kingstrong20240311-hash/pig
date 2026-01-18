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
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.exception.ErrorCodes;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.order.api.dto.*;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderCancel;
import com.pig4cloud.pig.order.api.entity.OrderFill;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.mapper.OrderCancelMapper;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.OrderService;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import com.pig4cloud.pig.vault.api.dto.CreateFreezeRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
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
import java.util.HashMap;
import java.util.Map;

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

	private final OrderFillMapper orderFillMapper;

	private final OrderCancelMapper orderCancelMapper;

	private final OutboxEventService outboxEventService;

	private final VaultService vaultService;

	@Value("${snowflake.worker-id:0}")
	private long workerId;

	@Value("${snowflake.datacenter-id:0}")
	private long datacenterId;

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
		Long orderId = IdUtil.getSnowflake(workerId, datacenterId).nextId();

		// 5. Create freeze in Vault
		CreateFreezeRequest freezeRequest = new CreateFreezeRequest();
		freezeRequest.setAccountId(request.getUserId());
		// TODO: get assetId from marketId mapping
		freezeRequest.setAssetId(1L);
		freezeRequest.setAmount(calculateFreezeAmount(request));
		freezeRequest.setRefType(RefType.ORDER);
		freezeRequest.setRefId(String.valueOf(orderId));

		R<FreezeResponse> freezeResult = vaultService.createFreeze(freezeRequest);
		if (!freezeResult.isSuccess()) {
			log.error("Failed to create freeze for order: {}, reason: {}", orderId, freezeResult.getMsg());
			return buildRejectedResponse(orderId, "Insufficient balance or freeze failed");
		}

		// 6. Create order entity in CREATED status
		Order order = new Order();
		order.setOrderId(orderId);
		order.setUserId(request.getUserId());
		order.setMarketId(request.getMarketId());
		order.setSide(request.getSide());
		order.setOrderType(request.getType());
		order.setPrice(request.getPrice());
		order.setQuantity(request.getQuantity());
		order.setRemainingQuantity(request.getQuantity());
		order.setStatus(OrderStatus.CREATED);
		order.setTimeInForce(request.getTimeInForce() != null ? request.getTimeInForce() : TimeInForce.GTC);
		order.setExpireAt(request.getExpireAt());
		order.setIdempotencyKey(idempotencyKey);
		order.setVersion(0);

		orderMapper.insert(order);

		// 7. Emit OrderCreatedEvent to Outbox
		publishOrderCreatedEvent(order);

		// 8. Transition to OPEN status (freeze is now in CREATED state, will be
		// CLAIMED by Vault upon receiving event)
		order.setStatus(OrderStatus.OPEN);
		orderMapper.updateById(order);

		// 9. TODO: Submit to matching engine (async, with retry)
		// This will be implemented later when matching engine integration is ready
		log.info("Order created and moved to OPEN: orderId={}, marketId={}, side={}, price={}, quantity={}", orderId,
				order.getMarketId(), order.getSide(), order.getPrice(), order.getQuantity());

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
		orderCancel.setCancelId(IdUtil.getSnowflake(workerId, datacenterId).nextId());
		orderCancel.setOrderId(request.getOrderId());
		orderCancel.setReason(request.getReason() != null ? request.getReason() : "User requested");
		orderCancel.setIdempotencyKey(idempotencyKey);
		orderCancelMapper.insert(orderCancel);

		// 6. Update order status to CANCEL_REQUESTED
		order.setStatus(OrderStatus.CANCEL_REQUESTED);
		orderMapper.updateById(order);

		// 7. Emit OrderCancelRequestedEvent
		publishOrderCancelRequestedEvent(order, idempotencyKey);

		// 8. TODO: Notify matching engine to apply cancel (async)
		// The actual cancellation will be done by matching thread calling applyCancel

		log.info("Order cancel requested: orderId={}, reason={}", order.getOrderId(), orderCancel.getReason());

		return buildCancelOrderResponse(order);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CommitMatchResponse commitMatch(CommitMatchRequest request) {
		// 1. Validate idempotency key must be provided
		String idempotencyKey = request.getIdempotencyKey();
		if (StrUtil.isBlank(idempotencyKey)) {
			throw new IllegalArgumentException("Idempotency key is required");
		}

		// 2. Check idempotency - if fills with this matchId exist, return existing
		// result
		long existingFillCount = orderFillMapper
			.selectCount(Wrappers.<OrderFill>lambdaQuery().eq(OrderFill::getMatchId, request.getMatchId()));

		if (existingFillCount > 0) {
			log.info("Match already committed for matchId: {}, returning existing state", request.getMatchId());
			return buildCommitMatchResponse(request);
		}

		// 3. Get taker order
		Order takerOrder = orderMapper.selectById(request.getTakerOrderId());
		if (takerOrder == null) {
			throw new IllegalArgumentException("Taker order not found: " + request.getTakerOrderId());
		}

		// 4. Process each fill
		Map<Long, OrderStateDTO> orderStates = new HashMap<>();
		BigDecimal totalTakerFilled = BigDecimal.ZERO;

		for (FillDTO fillDTO : request.getFills()) {
			// 4.1 Get maker order
			Order makerOrder = orderMapper.selectById(fillDTO.getMakerOrderId());
			if (makerOrder == null) {
				throw new IllegalArgumentException("Maker order not found: " + fillDTO.getMakerOrderId());
			}

			// 4.2 Insert fill record
			OrderFill fill = new OrderFill();
			fill.setTradeId(IdUtil.getSnowflake(workerId, datacenterId).nextId());
			fill.setMatchId(request.getMatchId());
			fill.setTakerOrderId(request.getTakerOrderId());
			fill.setMakerOrderId(fillDTO.getMakerOrderId());
			fill.setPrice(fillDTO.getPrice());
			fill.setQuantity(fillDTO.getQuantity());
			fill.setFee(fillDTO.getFee());
			orderFillMapper.insert(fill);

			// 4.3 Update maker order
			BigDecimal newMakerRemaining = makerOrder.getRemainingQuantity().subtract(fillDTO.getQuantity());

			// Validate remaining quantity: must not be negative
			if (newMakerRemaining.compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalStateException("Maker order remaining quantity cannot be negative: orderId="
						+ makerOrder.getOrderId() + ", remaining=" + makerOrder.getRemainingQuantity() + ", filled="
						+ fillDTO.getQuantity());
			}

			makerOrder.setRemainingQuantity(newMakerRemaining);
			if (newMakerRemaining.compareTo(BigDecimal.ZERO) == 0) {
				// Exactly 0: FILLED
				makerOrder.setStatus(OrderStatus.FILLED);
			}
			else {
				// Greater than 0: PARTIALLY_FILLED
				makerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
			}
			orderMapper.updateById(makerOrder);

			// Track maker order state
			orderStates.put(makerOrder.getOrderId(), buildOrderStateDTO(makerOrder));

			// Accumulate taker filled quantity
			totalTakerFilled = totalTakerFilled.add(fillDTO.getQuantity());
		}

		// 5. Update taker order
		BigDecimal newTakerRemaining = takerOrder.getRemainingQuantity().subtract(totalTakerFilled);

		// Validate remaining quantity: must not be negative
		if (newTakerRemaining.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalStateException(
					"Taker order remaining quantity cannot be negative: orderId=" + takerOrder.getOrderId()
							+ ", remaining=" + takerOrder.getRemainingQuantity() + ", filled=" + totalTakerFilled);
		}

		takerOrder.setRemainingQuantity(newTakerRemaining);
		if (newTakerRemaining.compareTo(BigDecimal.ZERO) == 0) {
			// Exactly 0: FILLED
			takerOrder.setStatus(OrderStatus.FILLED);
		}
		else {
			// Greater than 0: PARTIALLY_FILLED
			takerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
		}
		orderMapper.updateById(takerOrder);

		// Track taker order state
		orderStates.put(takerOrder.getOrderId(), buildOrderStateDTO(takerOrder));

		// 6. Emit MatchCommittedEvent
		publishMatchCommittedEvent(request, orderStates);

		log.info("Match committed: matchId={}, takerOrderId={}, fillCount={}, takerFilled={}", request.getMatchId(),
				request.getTakerOrderId(), request.getFills().size(), totalTakerFilled);

		// 7. Build response
		CommitMatchResponse response = new CommitMatchResponse();
		response.setMatchId(request.getMatchId());
		response.setOrderStates(orderStates);
		response.setSettlementRequired(true);

		return response;
	}

	/**
	 * Validate create order request
	 */
	private void validateCreateOrderRequest(CreateOrderRequest request) {
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
	 * Build OrderStateDTO from Order
	 */
	private OrderStateDTO buildOrderStateDTO(Order order) {
		OrderStateDTO dto = new OrderStateDTO();
		dto.setOrderId(order.getOrderId());
		dto.setStatus(order.getStatus());
		dto.setRemainingQuantity(order.getRemainingQuantity());
		return dto;
	}

	/**
	 * Build CommitMatchResponse (for idempotent replay)
	 */
	private CommitMatchResponse buildCommitMatchResponse(CommitMatchRequest request) {
		// Rebuild response from existing data
		Map<Long, OrderStateDTO> orderStates = new HashMap<>();

		Order takerOrder = orderMapper.selectById(request.getTakerOrderId());
		if (takerOrder != null) {
			orderStates.put(takerOrder.getOrderId(), buildOrderStateDTO(takerOrder));
		}

		for (FillDTO fill : request.getFills()) {
			Order makerOrder = orderMapper.selectById(fill.getMakerOrderId());
			if (makerOrder != null) {
				orderStates.put(makerOrder.getOrderId(), buildOrderStateDTO(makerOrder));
			}
		}

		CommitMatchResponse response = new CommitMatchResponse();
		response.setMatchId(request.getMatchId());
		response.setOrderStates(orderStates);
		response.setSettlementRequired(true);

		return response;
	}

	/**
	 * Publish OrderCreatedEvent to Outbox
	 */
	private void publishOrderCreatedEvent(Order order) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(IdUtil.randomUUID());
		event.setDomain(DOMAIN_ORDER);
		event.setAggregateType(AGG_TYPE_ORDER);
		event.setAggregateId(String.valueOf(order.getOrderId()));
		event.setEventType("OrderCreated");

		Map<String, Object> payload = new HashMap<>();
		payload.put("orderId", order.getOrderId());
		payload.put("userId", order.getUserId());
		payload.put("marketId", order.getMarketId());
		payload.put("status", order.getStatus());
		event.setPayloadJson(JSONUtil.toJsonStr(payload));

		event.setPartitionKey(String.valueOf(order.getOrderId()));
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());

		outboxEventService.save(event);
	}

	/**
	 * Publish OrderCancelRequestedEvent to Outbox
	 */
	private void publishOrderCancelRequestedEvent(Order order, String idempotencyKey) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(IdUtil.randomUUID());
		event.setDomain(DOMAIN_ORDER);
		event.setAggregateType(AGG_TYPE_ORDER);
		event.setAggregateId(String.valueOf(order.getOrderId()));
		event.setEventType("OrderCancelRequested");

		Map<String, Object> payload = new HashMap<>();
		payload.put("orderId", order.getOrderId());
		payload.put("status", order.getStatus());
		payload.put("idempotencyKey", idempotencyKey);
		event.setPayloadJson(JSONUtil.toJsonStr(payload));

		event.setPartitionKey(String.valueOf(order.getOrderId()));
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());

		outboxEventService.save(event);
	}

	/**
	 * Publish MatchCommittedEvent to Outbox
	 */
	private void publishMatchCommittedEvent(CommitMatchRequest request, Map<Long, OrderStateDTO> orderStates) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(IdUtil.randomUUID());
		event.setDomain(DOMAIN_ORDER);
		event.setAggregateType("Match");
		event.setAggregateId(request.getMatchId());
		event.setEventType("MatchCommitted");

		Map<String, Object> payload = new HashMap<>();
		payload.put("matchId", request.getMatchId());
		payload.put("takerOrderId", request.getTakerOrderId());
		payload.put("fills", request.getFills());
		payload.put("orderStates", orderStates);
		event.setPayloadJson(JSONUtil.toJsonStr(payload));

		event.setPartitionKey(request.getMatchId());
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());

		outboxEventService.save(event);
	}

}
