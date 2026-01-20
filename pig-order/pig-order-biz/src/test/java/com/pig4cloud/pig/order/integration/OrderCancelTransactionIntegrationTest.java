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
import com.pig4cloud.pig.order.api.dto.CancelOrderRequest;
import com.pig4cloud.pig.order.api.dto.CancelOrderResponse;
import com.pig4cloud.pig.order.api.dto.CreateOrderRequest;
import com.pig4cloud.pig.order.api.dto.CreateOrderResponse;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderCancel;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.mapper.OrderCancelMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.OrderService;
import com.pig4cloud.pig.outbox.dispatcher.OutboxEventDispatcher;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.cmd.CommandResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Order Cancellation and Transactions
 *
 * @author lengleng
 * @date 2025/01/19
 */
@DisplayName("Order Cancel and Transaction Integration Tests")
@Sql(scripts = { "classpath:db/schema.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = { "classpath:db/cleanup.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderCancelTransactionIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private OrderCancelMapper orderCancelMapper;

	@Autowired
	private OutboxEventDispatcher outboxEventDispatcher;

	@MockBean
	private VaultService vaultService;

	@SpyBean
	private ExchangeApi exchangeApi;

	@BeforeEach
	void setUp() {
		// Mock vault service to always return success
		FreezeResponse freezeResponse = new FreezeResponse();
		freezeResponse.setFreezeId(1000L);
		when(vaultService.createFreeze(any())).thenReturn(R.ok(freezeResponse));
	}

	/**
	 * I-04: Test normal order cancellation with database persistence
	 */
	@Test
	@DisplayName("I-04: Normal cancellation persisted correctly")
	void testNormalCancellationPersistence() {
		// Given: Create an order
		CreateOrderRequest createRequest = new CreateOrderRequest();
		createRequest.setUserId(100L);
		createRequest.setMarketId(1L);
		createRequest.setSide(Side.BUY);
		createRequest.setType(OrderType.LIMIT);
		createRequest.setPrice(new BigDecimal("100.00"));
		createRequest.setQuantity(new BigDecimal("10.00"));
		createRequest.setTimeInForce(TimeInForce.GTC);
		createRequest.setIdempotencyKey("order-to-cancel-1");

		CreateOrderResponse createResponse = orderService.createOrder(createRequest);
		assertThat(createResponse.getOrderId()).isNotNull();
		assertThat(createResponse.getStatus()).isEqualTo(OrderStatus.OPEN);

		final Long orderId = createResponse.getOrderId();

		// Dispatch events to process OrderCreated
		outboxEventDispatcher.dispatch();

		// Wait for order to be accepted into matching engine
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			assertThat(order.getStatus()).isIn(OrderStatus.MATCHING, OrderStatus.OPEN);
		});

		// When: Cancel the order
		CancelOrderRequest cancelRequest = new CancelOrderRequest();
		cancelRequest.setOrderId(orderId);
		cancelRequest.setReason("User requested");
		cancelRequest.setIdempotencyKey("cancel-request-1");

		CancelOrderResponse cancelResponse = orderService.cancelOrder(cancelRequest);
		assertThat(cancelResponse.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);

		// Dispatch events to process OrderCancelRequested
		outboxEventDispatcher.dispatch();

		// Then: Wait for matching engine callback and verify cancellation
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

			// Verify cancel record persisted
			OrderCancel cancel = orderCancelMapper
				.selectOne(new LambdaQueryWrapper<OrderCancel>().eq(OrderCancel::getOrderId, orderId));
			assertThat(cancel).isNotNull();
			assertThat(cancel.getReason()).isEqualTo("User requested");
		});
	}

	/**
	 * I-05: Test transaction rollback when order submission fails
	 */
	@Test
	@DisplayName("I-05: Order creation fails, status becomes REJECTED")
	void testOrderCreationFailureRollback() {
		// Given: Mock exchange API to fail
		doReturn(CompletableFuture.completedFuture(CommandResultCode.MATCHING_UNSUPPORTED_COMMAND)).when(exchangeApi)
			.submitCommandAsync(any());

		// When: Try to create order
		CreateOrderRequest createRequest = new CreateOrderRequest();
		createRequest.setUserId(100L);
		createRequest.setMarketId(1L);
		createRequest.setSide(Side.BUY);
		createRequest.setType(OrderType.LIMIT);
		createRequest.setPrice(new BigDecimal("100.00"));
		createRequest.setQuantity(new BigDecimal("10.00"));
		createRequest.setTimeInForce(TimeInForce.GTC);
		createRequest.setIdempotencyKey("failing-order-1");

		// Order is created in OPEN status
		CreateOrderResponse createResponse = orderService.createOrder(createRequest);
		assertThat(createResponse.getOrderId()).isNotNull();
		assertThat(createResponse.getStatus()).isEqualTo(OrderStatus.OPEN);

		Long orderId = createResponse.getOrderId();

		// Dispatch events to process OrderCreated (which will fail)
		outboxEventDispatcher.dispatch();

		// Then: Order status should become REJECTED
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
			assertThat(order.getRejectReason()).contains("Matching engine rejected");
		});
	}

	/**
	 * I-06: Test transaction rollback when cancel submission fails
	 */
	@Test
	@DisplayName("I-06: Cancel fails, status rolled back")
	void testCancelFailureRollback() {
		// Given: Create an order successfully first
		CreateOrderRequest createRequest = new CreateOrderRequest();
		createRequest.setUserId(100L);
		createRequest.setMarketId(1L);
		createRequest.setSide(Side.BUY);
		createRequest.setType(OrderType.LIMIT);
		createRequest.setPrice(new BigDecimal("100.00"));
		createRequest.setQuantity(new BigDecimal("10.00"));
		createRequest.setTimeInForce(TimeInForce.GTC);
		createRequest.setIdempotencyKey("order-to-cancel-2");

		CreateOrderResponse createResponse = orderService.createOrder(createRequest);
		Long orderId = createResponse.getOrderId();

		// Dispatch events to process OrderCreated
		outboxEventDispatcher.dispatch();

		// Wait for order to be accepted into matching engine
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			assertThat(order.getStatus()).isIn(OrderStatus.MATCHING, OrderStatus.OPEN);
		});

		// Mock exchange API to fail cancel (after order creation succeeds)
		doReturn(CompletableFuture.completedFuture(CommandResultCode.MATCHING_UNKNOWN_ORDER_ID)).when(exchangeApi)
			.submitCommandAsync(any());

		// When: Try to cancel order
		CancelOrderRequest cancelRequest = new CancelOrderRequest();
		cancelRequest.setOrderId(orderId);
		cancelRequest.setReason("User requested");
		cancelRequest.setIdempotencyKey("cancel-request-2");

		// Cancel request is accepted (returns CANCEL_REQUESTED)
		CancelOrderResponse cancelResponse = orderService.cancelOrder(cancelRequest);
		assertThat(cancelResponse.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);

		// Dispatch events to process OrderCancelRequested (which will fail and rollback)
		outboxEventDispatcher.dispatch();

		// Then: Wait for event handler to process and rollback status
		// Event handler will fail and rollback status to MATCHING
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order order = orderMapper.selectById(orderId);
			// Status should be rolled back to MATCHING (not CANCELLED)
			assertThat(order.getStatus()).isEqualTo(OrderStatus.MATCHING);
		});
	}

}
