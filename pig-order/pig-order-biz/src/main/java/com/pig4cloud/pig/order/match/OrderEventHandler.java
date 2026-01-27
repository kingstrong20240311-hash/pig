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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCancelPayload;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCancelRequestedPayload;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCreatedPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.api.ApiCancelOrder;
import exchange.core2.core.common.api.ApiPlaceOrder;
import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	private final MatchingEngineProperties matchingEngineProperties;

	private final MarketService marketService;

	private final DomainEventPublisher domainEventPublisher;

	private final ObjectMapper objectMapper;

	private static final String DOMAIN_ORDER = "order";

	private static final String AGG_TYPE_ORDER = "Order";

	private static final String EVENT_ORDER_CANCEL = "OrderCancel";

	/**
	 * Handle OrderCreated event - Submit order to matching engine asynchronously
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderCreated")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrderCreated(DomainEventEnvelope<OrderCreatedPayload> event) {
		log.info("Handling OrderCreated event: eventId={}, aggregateId={}", event.eventId(), event.aggregateId());

		try {
			// 1. Parse payload to get orderId
			OrderCreatedPayload payload = event.payloadAs(objectMapper, OrderCreatedPayload.class);
			Long orderId = payload == null ? null : payload.getOrderId();
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
				// Special case: If order was cancelled before being submitted to matching
				// engine
				if (order.getStatus() == OrderStatus.CANCEL_REQUESTED) {
					log.info(
							"Order was cancelled before submission to matching engine: orderId={}, marking as CANCELLED directly",
							orderId);
					order.setStatus(OrderStatus.CANCELLED);
					orderMapper.updateById(order);
					publishOrderCancelEvent(order);
				}
				else {
					log.info("Order already processed or in non-OPEN state: orderId={}, status={}, skipping", orderId,
							order.getStatus());
				}
				return;
			}

			// 4. Submit order to matching engine
			int symbolId = resolveSymbolId(order);
			matchingEngineSymbolService.ensureSymbol(symbolId, symbolId, matchingEngineProperties.getDefaultAsset());
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
						log.info("Order status changed before MATCHING transition: orderId={}, status={}, eventId={}",
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
	public void handleOrderCancelRequested(DomainEventEnvelope<OrderCancelRequestedPayload> event) {
		log.info("Handling OrderCancelRequested event: eventId={}, aggregateId={}", event.eventId(),
				event.aggregateId());

		try {
			// 1. Parse payload
			OrderCancelRequestedPayload payload = event.payloadAs(objectMapper, OrderCancelRequestedPayload.class);
			Long orderId = payload == null ? null : payload.getOrderId();
			Long userId = payload == null ? null : payload.getUserId();
			Long marketId = payload == null ? null : payload.getMarketId();

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
			int symbolId = resolveSymbolId(order);
			ApiCancelOrder cancelOrderCmd = OrderCommandConverter.toApiCancelOrder(orderId, userId, symbolId);
			CommandResultCode resultCode = exchangeApi.submitCommandAsync(cancelOrderCmd).join();

			if (resultCode == CommandResultCode.SUCCESS) {
				log.info("Cancel request submitted to matching engine successfully: orderId={}, eventId={}", orderId,
						event.eventId());
				// Status will be updated by reduce event callback
			}
			else if (resultCode == CommandResultCode.MATCHING_UNKNOWN_ORDER_ID) {
				// Order was never submitted to matching engine (race condition)
				// Mark as CANCELLED directly since there's nothing to cancel in the
				// engine
				log.info(
						"Order not found in matching engine (never submitted): orderId={}, marking as CANCELLED directly",
						orderId);
				order.setStatus(OrderStatus.CANCELLED);
				orderMapper.updateById(order);
				publishOrderCancelEvent(order);
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

	private void publishOrderCancelEvent(Order order) {
		OrderCancelPayload payload = new OrderCancelPayload(order.getOrderId(), order.getUserId(), order.getMarketId(),
				order.getStatus().name(), order.getRejectReason());

		DomainEventEnvelope<OrderCancelPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				EVENT_ORDER_CANCEL, // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	private int resolveSymbolId(Order order) {
		if (order.getOutcome() == null) {
			throw new IllegalStateException("Order outcome is required: orderId=" + order.getOrderId());
		}

		com.pig4cloud.pig.order.api.entity.Market market = marketService.getMarket(order.getMarketId());
		if (market == null) {
			throw new IllegalStateException("Market not found: " + order.getMarketId());
		}

		Integer symbolId = order.getOutcome() == Outcome.YES ? market.getSymbolIdYes() : market.getSymbolIdNo();
		if (symbolId == null) {
			throw new IllegalStateException("Market symbols not ready: marketId=" + order.getMarketId());
		}
		return symbolId;
	}

}
