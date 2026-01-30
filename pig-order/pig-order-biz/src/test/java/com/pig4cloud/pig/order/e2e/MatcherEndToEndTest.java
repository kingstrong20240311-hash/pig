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

package com.pig4cloud.pig.order.e2e;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.order.BaseIntegrationTest;
import com.pig4cloud.pig.order.api.dto.CancelOrderRequest;
import com.pig4cloud.pig.order.api.dto.CancelOrderResponse;
import com.pig4cloud.pig.order.api.dto.CreateOrderRequest;
import com.pig4cloud.pig.order.api.dto.CreateOrderResponse;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderFill;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.OrderService;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end tests for Matcher integration Tests real matching engine behavior without
 * mocks
 *
 * @author lengleng
 * @date 2025/01/19
 */
@DisplayName("Matcher End-to-End Tests")
@Sql(scripts = { "classpath:db/schema.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = { "classpath:db/cleanup.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MatcherEndToEndTest extends BaseIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private OrderFillMapper orderFillMapper;

	@MockBean
	private VaultService vaultService;

	@BeforeEach
	void setUp() {
		// Mock vault service to always return success
		FreezeResponse freezeResponse = new FreezeResponse();
		freezeResponse.setFreezeId(1000L);
		when(vaultService.createFreeze(any())).thenReturn(R.ok(freezeResponse));
	}

	/**
	 * E-01: Test price-time priority matching Multiple orders at different prices should
	 * match in correct order
	 */
	@Test
	@DisplayName("E-01: Price-time priority matching order")
	void testPriceTimePriorityMatching() {
		// Given: Create multiple SELL orders at different prices
		// Order 1: Price 102, Time T1
		CreateOrderRequest sell1 = createOrder(Side.SELL, new BigDecimal("102.00"), new BigDecimal("5.00"), "sell-1");
		orderService.createOrder(sell1);

		// Order 2: Price 100, Time T2 (better price, later time)
		CreateOrderRequest sell2 = createOrder(Side.SELL, new BigDecimal("100.00"), new BigDecimal("5.00"), "sell-2");
		CreateOrderResponse sellResp2 = orderService.createOrder(sell2);

		// Order 3: Price 101, Time T3
		CreateOrderRequest sell3 = createOrder(Side.SELL, new BigDecimal("101.00"), new BigDecimal("5.00"), "sell-3");
		orderService.createOrder(sell3);

		// Order 4: Price 100, Time T4 (same price as Order 2, later time)
		CreateOrderRequest sell4 = createOrder(Side.SELL, new BigDecimal("100.00"), new BigDecimal("5.00"), "sell-4");
		CreateOrderResponse sellResp4 = orderService.createOrder(sell4);

		// When: Create a large BUY order that can match all
		CreateOrderRequest buy = createOrder(Side.BUY, new BigDecimal("105.00"), new BigDecimal("20.00"), "buy-1");
		CreateOrderResponse buyResp = orderService.createOrder(buy);

		// Then: Wait for matching and verify order
		// Expected matching order: sell2 (100, T2) -> sell4 (100, T4) -> sell3 (101,
		// T3) -> sell1 (102, T1)
		final Long buyOrderId = buyResp.getOrderId();

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order buyOrder = orderMapper.selectById(buyOrderId);
			assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.FILLED);

			// Get all fills for this buy order, sorted by trade_id (creation order)
			List<OrderFill> fills = orderFillMapper
				.selectList(new LambdaQueryWrapper<OrderFill>().eq(OrderFill::getTakerOrderId, buyOrderId)
					.orderByAsc(OrderFill::getTradeId));

			assertThat(fills).hasSize(4);

			// Verify price priority: 100, 100, 101, 102
			assertThat(fills.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
			assertThat(fills.get(1).getPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
			assertThat(fills.get(2).getPrice()).isEqualByComparingTo(new BigDecimal("101.00"));
			assertThat(fills.get(3).getPrice()).isEqualByComparingTo(new BigDecimal("102.00"));

			// Verify time priority for same price (100): sell2 before sell4
			// sell2 (orderId smaller, created earlier) should match before sell4
			assertThat(fills.get(0).getMakerOrderId()).isEqualTo(sellResp2.getOrderId());
			assertThat(fills.get(1).getMakerOrderId()).isEqualTo(sellResp4.getOrderId());
		});
	}

	/**
	 * E-02: Test IOC order rejection when no counterparty exists
	 */
	@Test
	@DisplayName("E-02: IOC order rejected when no match available")
	void testIOCOrderRejection() {
		// Given: No existing orders in the book

		// When: Create an IOC BUY order
		CreateOrderRequest iocBuy = new CreateOrderRequest();
		iocBuy.setUserId(100L);
		iocBuy.setMarketId(1L);
		iocBuy.setOutcome(Outcome.YES);
		iocBuy.setSide(Side.BUY);
		iocBuy.setType(OrderType.LIMIT);
		iocBuy.setPrice(new BigDecimal("100.00"));
		iocBuy.setQuantity(new BigDecimal("10.00"));
		iocBuy.setTimeInForce(TimeInForce.IOC);
		iocBuy.setIdempotencyKey("ioc-reject-1");

		CreateOrderResponse iocResp = orderService.createOrder(iocBuy);

		// Then: Wait for reject event to be processed
		final Long orderId = iocResp.getOrderId();

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			assertThat(order).isNotNull();
			assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
			assertThat(order.getRejectReason()).contains("IOC order could not be filled");
		});
	}

	/**
	 * E-03: Test reduce event when order is cancelled
	 */
	@Test
	@DisplayName("E-03: Reduce event triggers order cancellation correctly")
	void testReduceEventOnCancellation() {
		// Given: Create a SELL order
		CreateOrderRequest sell = createOrder(Side.SELL, new BigDecimal("100.00"), new BigDecimal("10.00"),
				"sell-reduce-1");
		CreateOrderResponse sellResp = orderService.createOrder(sell);

		// Verify order is in MATCHING status
		await().atMost(2, TimeUnit.SECONDS)
			.untilAsserted(() -> assertThat(orderMapper.selectById(sellResp.getOrderId()).getStatus())
				.isEqualTo(OrderStatus.MATCHING));

		// When: Cancel the order
		CancelOrderRequest cancelReq = new CancelOrderRequest();
		cancelReq.setOrderId(sellResp.getOrderId());
		cancelReq.setReason("User requested");
		cancelReq.setIdempotencyKey("cancel-reduce-1");

		CancelOrderResponse cancelResp = orderService.cancelOrder(cancelReq);
		assertThat(cancelResp.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);

		// Then: Wait for reduce event to trigger cancellation
		final Long orderId = sellResp.getOrderId();

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
			// remainingQuantity preserved (amount not filled when cancelled)
			assertThat(order.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("10.00"));
		});
	}

	/**
	 * Helper method to create order request
	 */
	private CreateOrderRequest createOrder(Side side, BigDecimal price, BigDecimal quantity, String idempotencyKey) {
		CreateOrderRequest request = new CreateOrderRequest();
		request.setUserId(100L);
		request.setMarketId(1L);
		request.setOutcome(Outcome.YES);
		request.setSide(side);
		request.setType(OrderType.LIMIT);
		request.setPrice(price);
		request.setQuantity(quantity);
		request.setTimeInForce(TimeInForce.GTC);
		request.setIdempotencyKey(idempotencyKey);
		return request;
	}

}
