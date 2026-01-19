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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.api.ApiPlaceOrder;
import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Recover matching engine state from order table when application is ready
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void recoverState() {
		if (!matchingEngineProperties.isEnableStateRecovery()) {
			log.info("State recovery is disabled, skipping...");
			return;
		}

		log.info("Starting matching engine state recovery from order table...");

		try {
			List<Order> allOrders = new ArrayList<>();

			// 1. First recover MATCHING status orders
			List<Order> matchingOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
				.eq(Order::getStatus, OrderStatus.MATCHING)
				.orderByAsc(Order::getCreateTime));

			log.info("Found {} MATCHING orders to recover", matchingOrders.size());
			allOrders.addAll(matchingOrders);

			// 2. Then recover OPEN and PARTIALLY_FILLED orders
			List<Order> activeOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
				.in(Order::getStatus, OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)
				.orderByAsc(Order::getCreateTime));

			log.info("Found {} OPEN/PARTIALLY_FILLED orders to recover", activeOrders.size());
			allOrders.addAll(activeOrders);

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

		int symbolId = order.getMarketId().intValue();

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

		log.debug("Order recovered successfully: orderId={}", order.getOrderId());
	}

}
