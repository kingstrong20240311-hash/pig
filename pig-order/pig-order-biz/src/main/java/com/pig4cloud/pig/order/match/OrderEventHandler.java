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

package com.pig4cloud.pig.order.match;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.api.ApiCancelOrder;
import exchange.core2.core.common.api.ApiPlaceOrder;
import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Order Event Handler - Handles order lifecycle events from outbox
 *
 * @author lengleng
 * @date 2025/01/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventHandler {

	private final OrderMapper orderMapper;

	private final ExchangeApi exchangeApi;

	private final MatchingEngineSymbolService matchingEngineSymbolService;

	private final DomainEventPublisher domainEventPublisher;

	private static final String DOMAIN_ORDER = "order";

	private static final String AGG_TYPE_ORDER = "Order";

	private static final String EVENT_ORDER_CANCEL = "OrderCancel";

	/**
	 * Handle OrderCreated event - Submit order to matching engine asynchronously
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderCreated")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrderCreated(DomainEventEnvelope event) {
		log.info("Handling OrderCreated event: eventId={}, aggregateId={}", event.eventId(), event.aggregateId());

		try {
			// 1. Parse payload to get orderId
			Long orderId = extractLong(event.payloadJson(), "orderId");
			if (orderId == null) {
				log.error("Failed to extract orderId from event payload: eventId={}", event.eventId());
				throw new IllegalArgumentException("Invalid payload: orderId not found");
			}

			// 2. Get order from database
			Order order = orderMapper.selectById(orderId);
			if (order == null) {
				log.error("Order not found for OrderCreated event: orderId={}, eventId={}", orderId, event.eventId());
				throw new IllegalArgumentException("Order not found: " + orderId);
			}

			// 3. Check order status - only process OPEN orders
			if (order.getStatus() != OrderStatus.OPEN) {
				log.info("Order already processed or in non-OPEN state: orderId={}, status={}, skipping", orderId,
						order.getStatus());
				return;
			}

			// 4. Submit order to matching engine
			int symbolId = order.getMarketId().intValue();
			matchingEngineSymbolService.ensureSymbol(symbolId);
			ApiPlaceOrder placeOrderCmd = OrderCommandConverter.toApiPlaceOrder(order, symbolId);
			CommandResultCode resultCode = exchangeApi.submitCommandAsync(placeOrderCmd).join();

			if (resultCode == CommandResultCode.SUCCESS) {
				// 5. For LIMIT orders, update status to MATCHING if still OPEN
				if (order.getOrderType() == com.pig4cloud.pig.order.api.enums.OrderType.LIMIT) {
					if (order.getStatus() == OrderStatus.OPEN) {
						order.setStatus(OrderStatus.MATCHING);
						orderMapper.updateById(order);
						log.info("Order submitted to matching engine successfully: orderId={}, eventId={}", orderId,
								event.eventId());
					}
					else {
						log.info(
								"Order status changed before MATCHING transition: orderId={}, status={}, eventId={}",
								orderId, order.getStatus(), event.eventId());
					}
				}
				else {
					log.info("Market order submitted to matching engine: orderId={}, eventId={}, status stays {}",
							orderId, event.eventId(), order.getStatus());
				}
			}
			else {
				// Matching engine rejected - mark order as REJECTED
				order.setStatus(OrderStatus.REJECTED);
				order.setRejectReason("Matching engine rejected: " + resultCode);
				orderMapper.updateById(order);
				publishOrderCancelEvent(order);

				log.warn("Order rejected by matching engine: orderId={}, resultCode={}", orderId, resultCode);
			}
		}
		catch (Exception e) {
			log.error("Failed to handle OrderCreated event: eventId={}, aggregateId={}", event.eventId(),
					event.aggregateId(), e);
			// Rethrowing will mark outbox event as failed and allow retries
			throw new RuntimeException("Failed to process OrderCreated event", e);
		}
	}

	/**
	 * Handle OrderCancelRequested event - Submit cancel to matching engine asynchronously
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderCancelRequested")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrderCancelRequested(DomainEventEnvelope event) {
		log.info("Handling OrderCancelRequested event: eventId={}, aggregateId={}", event.eventId(),
				event.aggregateId());

		try {
			// 1. Parse payload
			Long orderId = extractLong(event.payloadJson(), "orderId");
			Long userId = extractLong(event.payloadJson(), "userId");
			Long marketId = extractLong(event.payloadJson(), "marketId");

			if (orderId == null || userId == null || marketId == null) {
				log.error("Invalid OrderCancelRequested payload: eventId={}, payload={}", event.eventId(),
						event.payloadJson());
				throw new IllegalArgumentException("Invalid payload");
			}

			// 2. Get order from database
			Order order = orderMapper.selectById(orderId);
			if (order == null) {
				log.error("Order not found for OrderCancelRequested event: orderId={}, eventId={}", orderId,
						event.eventId());
				throw new IllegalArgumentException("Order not found: " + orderId);
			}

			// 3. Check order status - only process CANCEL_REQUESTED orders
			if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
				log.info("Order not in CANCEL_REQUESTED state: orderId={}, status={}, skipping", orderId,
						order.getStatus());
				return;
			}

			// 4. Submit cancel to matching engine
			int symbolId = marketId.intValue();
			ApiCancelOrder cancelOrderCmd = OrderCommandConverter.toApiCancelOrder(orderId, userId, symbolId);
			CommandResultCode resultCode = exchangeApi.submitCommandAsync(cancelOrderCmd).join();

			if (resultCode == CommandResultCode.SUCCESS) {
				log.info("Cancel request submitted to matching engine successfully: orderId={}, eventId={}", orderId,
						event.eventId());
				// Status will be updated by reduce event callback
			}
			else {
				log.error("Failed to submit cancel to matching engine: orderId={}, resultCode={}", orderId, resultCode);
				// Rollback order status to previous state
				order.setStatus(OrderStatus.MATCHING);
				orderMapper.updateById(order);
				log.warn("Order cancel request failed, status rolled back to MATCHING: orderId={}", orderId);
			}
		}
		catch (Exception e) {
			log.error("Failed to handle OrderCancelRequested event: eventId={}, aggregateId={}", event.eventId(),
					event.aggregateId(), e);
			throw new RuntimeException("Failed to process OrderCancelRequested event", e);
		}
	}

	/**
	 * Extract Long value from JSON payload
	 */
	private Long extractLong(String payloadJson, String key) {
		try {
			Map<String, Object> payload = JSONUtil.toBean(payloadJson, Map.class);
			Object value = payload.get(key);
			if (value instanceof Number) {
				return ((Number) value).longValue();
			}
			else if (value instanceof String) {
				return Long.parseLong((String) value);
			}
			return null;
		}
		catch (Exception e) {
			log.error("Failed to parse payload: key={}", key, e);
			return null;
		}
	}

	private void publishOrderCancelEvent(Order order) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("orderId", order.getOrderId());
		payload.put("userId", order.getUserId());
		payload.put("marketId", order.getMarketId());
		payload.put("status", order.getStatus().name());
		payload.put("reason", order.getRejectReason());

		DomainEventEnvelope event = new DomainEventEnvelope(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				EVENT_ORDER_CANCEL, // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				JSONUtil.toJsonStr(payload) // payloadJson
		);

		domainEventPublisher.publish(event);
	}

}
