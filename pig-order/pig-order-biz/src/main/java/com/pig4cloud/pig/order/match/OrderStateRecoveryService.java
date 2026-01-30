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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.api.enums.MarketStatus;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderReducedPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.api.ApiPlaceOrder;
import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.pig4cloud.pig.order.match.event.ExchangeCoreInitedEvent;

/**
 * Order State Recovery Service - Recovers matching engine state from order table on
 * startup
 *
 * @author lengleng
 * @date 2025/01/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateRecoveryService {

	private final OrderMapper orderMapper;

	private final ExchangeApi exchangeApi;

	private final MatchingEngineProperties matchingEngineProperties;

	private final MarketService marketService;

	private final MatchingEngineSymbolService matchingEngineSymbolService;

	private final ApplicationEventPublisher eventPublisher;

	private final DomainEventPublisher domainEventPublisher;

	private static final String DOMAIN_ORDER = "order";

	private static final String AGG_TYPE_ORDER = "Order";

	private static final String EVENT_ORDER_REDUCED = "OrderReduced";

	@EventListener(ApplicationReadyEvent.class)
	@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
	public void refuseTrafficOnStartup() {
		AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
	}

	/**
	 * Recover matching engine state from order table when application is ready
	 */
	@EventListener(ExchangeCoreInitedEvent.class)
	public void recoverState(ExchangeCoreInitedEvent event) {
		if (!matchingEngineProperties.isEnableStateRecovery()) {
			log.info("State recovery is disabled, skipping...");
			AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
			return;
		}

		log.info("Starting matching engine state recovery from order table...");

		try {
			List<Order> allOrders = new ArrayList<>();

			// 1. First recover OPEN status orders (not yet submitted or submission not
			// confirmed)
			List<Order> openOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
				.eq(Order::getStatus, OrderStatus.OPEN)
				.orderByAsc(Order::getCreateTime));

			log.info("Found {} OPEN orders to recover (will submit to matching engine)", openOrders.size());
			allOrders.addAll(openOrders);

			// 2. Recover MATCHING status orders (submitted but may need re-submission)
			List<Order> matchingOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
				.eq(Order::getStatus, OrderStatus.MATCHING)
				.orderByAsc(Order::getCreateTime));

			log.info("Found {} MATCHING orders to recover", matchingOrders.size());
			allOrders.addAll(matchingOrders);

			// 3. Recover PARTIALLY_FILLED orders
			List<Order> partiallyFilledOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
				.eq(Order::getStatus, OrderStatus.PARTIALLY_FILLED)
				.orderByAsc(Order::getCreateTime));

			log.info("Found {} PARTIALLY_FILLED orders to recover", partiallyFilledOrders.size());
			allOrders.addAll(partiallyFilledOrders);

			// Note: FAILED orders are NOT recovered - they remain in FAILED state

			int successCount = 0;
			int failureCount = 0;

			// Resubmit each order to matching engine
			for (Order order : allOrders) {
				try {
					recoverOrder(order);
					successCount++;
				}
				catch (Exception e) {
					log.error("Failed to recover order: orderId={}, status={}", order.getOrderId(), order.getStatus(),
							e);
					failureCount++;
					// TODO: Handle recovery failures - may need to mark order as error
					// state
				}
			}

			log.info("State recovery completed: total={}, success={}, failures={}", allOrders.size(), successCount,
					failureCount);
			AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
		}
		catch (Exception e) {
			log.error("Fatal error during state recovery", e);
			throw new RuntimeException("State recovery failed", e);
		}
	}

	/**
	 * Recover a single order by resubmitting it to matching engine
	 */
	private void recoverOrder(Order order) {
		log.debug("Recovering order: orderId={}, status={}, remaining={}", order.getOrderId(), order.getStatus(),
				order.getRemainingQuantity());

		Market market = marketService.getMarket(order.getMarketId());
		if (market == null || market.getStatus() != MarketStatus.ACTIVE
				|| (market.getExpireAt() != null && !market.getExpireAt().isAfter(Instant.now()))) {
			order.setStatus(OrderStatus.EXPIRED);
			orderMapper.updateById(order);
			publishOrderReducedEvent(order);
			log.info("Skipping recovery for expired market order: orderId={}, marketId={}", order.getOrderId(),
					order.getMarketId());
			return;
		}

		int symbolId = resolveSymbolId(order, market);
		matchingEngineSymbolService.ensureSymbol(symbolId, symbolId, matchingEngineProperties.getDefaultAsset());

		// For OPEN orders, we need to update status to MATCHING after successful
		// submission
		OrderStatus previousStatus = order.getStatus();

		// Create a modified order with remaining quantity
		Order recoveryOrder = new Order();
		recoveryOrder.setOrderId(order.getOrderId());
		recoveryOrder.setUserId(order.getUserId());
		recoveryOrder.setMarketId(order.getMarketId());
		recoveryOrder.setSide(order.getSide());
		recoveryOrder.setOrderType(order.getOrderType());
		recoveryOrder.setPrice(order.getPrice());
		// Use remaining quantity, not original quantity
		recoveryOrder.setQuantity(order.getRemainingQuantity());
		recoveryOrder.setTimeInForce(order.getTimeInForce());

		ApiPlaceOrder placeOrderCmd = OrderCommandConverter.toApiPlaceOrder(recoveryOrder, symbolId);
		CommandResultCode resultCode = exchangeApi.submitCommandAsync(placeOrderCmd).join();

		if (resultCode != CommandResultCode.SUCCESS) {
			throw new IllegalStateException(
					"Failed to recover order: orderId=" + order.getOrderId() + ", resultCode=" + resultCode);
		}

		// Update OPEN orders to MATCHING after successful submission
		if (previousStatus == OrderStatus.OPEN) {
			order.setStatus(OrderStatus.MATCHING);
			orderMapper.updateById(order);
			log.info("Order recovered and status updated: orderId={}, OPEN -> MATCHING", order.getOrderId());
		}

		log.debug("Order recovered successfully: orderId={}", order.getOrderId());
	}

	private void publishOrderReducedEvent(Order order) {
		// 资产金额：BUY = 剩余数量×价格，SELL = 剩余数量
		BigDecimal amount = order.getPrice() != null ? order.getRemainingQuantity().multiply(order.getPrice())
				: order.getRemainingQuantity();
		OrderReducedPayload payload = new OrderReducedPayload(order.getOrderId(), amount);

		DomainEventEnvelope<OrderReducedPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				EVENT_ORDER_REDUCED, // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	private int resolveSymbolId(Order order, Market market) {
		if (order.getOutcome() == null) {
			throw new IllegalStateException("Order outcome is required: orderId=" + order.getOrderId());
		}
		Integer symbolId = order.getOutcome() == Outcome.YES ? market.getSymbolIdYes() : market.getSymbolIdNo();
		if (symbolId == null) {
			throw new IllegalStateException("Market symbols not ready: marketId=" + market.getMarketId());
		}
		return symbolId;
	}

}
