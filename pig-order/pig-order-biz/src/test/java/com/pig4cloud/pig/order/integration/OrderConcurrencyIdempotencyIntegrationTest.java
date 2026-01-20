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

package com.pig4cloud.pig.order.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.order.BaseIntegrationTest;
import com.pig4cloud.pig.order.api.dto.CreateOrderRequest;
import com.pig4cloud.pig.order.api.dto.CreateOrderResponse;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderFill;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.match.OrderMatchService;
import com.pig4cloud.pig.order.service.OrderService;
import com.pig4cloud.pig.outbox.dispatcher.OutboxEventDispatcher;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import exchange.core2.core.IEventsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Concurrency and Idempotency
 *
 * @author lengleng
 * @date 2025/01/19
 */
@DisplayName("Concurrency and Idempotency Integration Tests")
@Sql(scripts = { "classpath:db/schema.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = { "classpath:db/cleanup.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderConcurrencyIdempotencyIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private OrderFillMapper orderFillMapper;

	@SpyBean
	private OrderMatchService orderMatchService;

	@MockBean
	private VaultService vaultService;

	@Autowired
	private OutboxEventDispatcher outboxEventDispatcher;

	@BeforeEach
	void setUp() {
		// Mock vault service to always return success
		FreezeResponse freezeResponse = new FreezeResponse();
		freezeResponse.setFreezeId(1000L);
		when(vaultService.createFreeze(any())).thenReturn(R.ok(freezeResponse));
	}

	/**
	 * I-09: Test concurrent order creation without duplicates
	 */
	@Test
	@DisplayName("I-09: Concurrent order creation without duplicates or dirty writes")
	void testConcurrentOrderCreation() throws InterruptedException {
		// Given: 10 threads trying to create orders concurrently
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		// When: Create orders concurrently with different price/quantity per thread
		for (int i = 0; i < threadCount; i++) {
			final int index = i;
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					CreateOrderRequest request = new CreateOrderRequest();
					request.setUserId(100L + index); // Different users
					request.setMarketId(1L);
					request.setSide(index % 2 == 0 ? Side.BUY : Side.SELL);
					request.setType(OrderType.LIMIT);
					// Price varies by thread: 100 + index (e.g., 100, 101, 102, ...)
					request.setPrice(new BigDecimal("100.00").add(new BigDecimal(index)));
					// Quantity varies by thread: 10 + index (e.g., 10, 11, 12, ...)
					request.setQuantity(new BigDecimal("10.00").add(new BigDecimal(index)));
					request.setTimeInForce(TimeInForce.GTC);
					request.setIdempotencyKey("concurrent-order-" + index);

					CreateOrderResponse response = orderService.createOrder(request);
					assertThat(response).isNotNull();
					assertThat(response.getOrderId()).isNotNull();
					successCount.incrementAndGet();
				}
				catch (Exception e) {
					failureCount.incrementAndGet();
				}
			}, executor);
			futures.add(future);
		}

		// Wait for all threads to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);

		// Manually trigger outbox event processing to submit orders to matching engine
		outboxEventDispatcher.dispatch();

		// Then: All orders should be created successfully
		assertThat(successCount.get()).isEqualTo(threadCount);
		assertThat(failureCount.get()).isZero();

		// Verify no duplicate orders in database
		long orderCount = orderMapper.selectCount(null);
		assertThat(orderCount).isEqualTo(threadCount);

		// Verify each order has correct data (no dirty writes)
		for (int i = 0; i < threadCount; i++) {
			Order order = orderMapper
				.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getIdempotencyKey, "concurrent-order-" + i));
			assertThat(order).isNotNull();
			assertThat(order.getUserId()).isEqualTo(100L + i);
			// Verify price = 100 + i
			assertThat(order.getPrice()).isEqualByComparingTo(new BigDecimal("100.00").add(new BigDecimal(i)));
			// Verify quantity = 10 + i
			assertThat(order.getQuantity()).isEqualByComparingTo(new BigDecimal("10.00").add(new BigDecimal(i)));
		}
	}

	/**
	 * I-10: Test idempotent fill processing (duplicate callbacks)
	 */
	@Test
	@DisplayName("I-10: Duplicate match callbacks don't create duplicate fills")
	void testIdempotentFillProcessing() {
		// Given: Create two orders that will match
		CreateOrderRequest makerRequest = new CreateOrderRequest();
		makerRequest.setUserId(100L);
		makerRequest.setMarketId(1L);
		makerRequest.setSide(Side.SELL);
		makerRequest.setType(OrderType.LIMIT);
		makerRequest.setPrice(new BigDecimal("100.00"));
		makerRequest.setQuantity(new BigDecimal("10.00"));
		makerRequest.setTimeInForce(TimeInForce.GTC);
		makerRequest.setIdempotencyKey("idempotency-maker-1");

		CreateOrderResponse makerResponse = orderService.createOrder(makerRequest);

		CreateOrderRequest takerRequest = new CreateOrderRequest();
		takerRequest.setUserId(200L);
		takerRequest.setMarketId(1L);
		takerRequest.setSide(Side.BUY);
		takerRequest.setType(OrderType.LIMIT);
		takerRequest.setPrice(new BigDecimal("100.00"));
		takerRequest.setQuantity(new BigDecimal("5.00"));
		takerRequest.setTimeInForce(TimeInForce.GTC);
		takerRequest.setIdempotencyKey("idempotency-taker-1");

		CreateOrderResponse takerResponse = orderService.createOrder(takerRequest);

		// Manually trigger outbox event processing to submit orders to matching engine
		outboxEventDispatcher.dispatch();

		// Capture the TradeEvent that will be called by matching engine
		ArgumentCaptor<IEventsHandler.TradeEvent> tradeEventCaptor = ArgumentCaptor
			.forClass(IEventsHandler.TradeEvent.class);

		// Wait for the first trade event to be processed
		final Long takerOrderId = takerResponse.getOrderId();
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(orderMatchService, atLeastOnce()).handleTradeEvent(tradeEventCaptor.capture());
			List<IEventsHandler.TradeEvent> events = tradeEventCaptor.getAllValues();
			assertThat(events.stream().anyMatch(e -> e.takerOrderId == takerOrderId)).isTrue();
		});

		// When: Replay the same TradeEvent multiple times (simulating duplicate
		// callbacks)
		List<IEventsHandler.TradeEvent> capturedEvents = tradeEventCaptor.getAllValues();
		IEventsHandler.TradeEvent originalEvent = capturedEvents.stream()
			.filter(e -> e.takerOrderId == takerOrderId)
			.findFirst()
			.orElseThrow();

		// Replay the event 2 more times
		orderMatchService.handleTradeEvent(originalEvent);
		orderMatchService.handleTradeEvent(originalEvent);

		// Then: Only one fill record should exist per maker-taker pair (idempotent)
		String matchId = String.format("%d-%d", originalEvent.takerOrderId, originalEvent.timestamp);
		List<OrderFill> fills = orderFillMapper
			.selectList(new LambdaQueryWrapper<OrderFill>().eq(OrderFill::getMatchId, matchId));

		assertThat(fills).hasSize(1);
		assertThat(fills.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("5.00"));

		// Verify order states are consistent despite replays
		Order takerOrder = orderMapper.selectById(takerResponse.getOrderId());
		assertThat(takerOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
		assertThat(takerOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);

		Order makerOrder = orderMapper.selectById(makerResponse.getOrderId());
		assertThat(makerOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
		assertThat(makerOrder.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("5.00"));
	}

}
