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
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.OrderService;
import com.pig4cloud.pig.outbox.dispatcher.OutboxEventDispatcher;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Order and Matching
 *
 * @author lengleng
 * @date 2025/01/19
 */
@DisplayName("Order Matching Integration Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = { "classpath:db/schema.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = { "classpath:db/cleanup.sql" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderMatchingIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private OrderFillMapper orderFillMapper;

	@Autowired
	private OutboxEventDispatcher outboxEventDispatcher;

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
	 * I-01: Test order creation and matching with database persistence
	 */
	@Test
	@DisplayName("I-01: Order creation → Matching → Fill persisted")
	void testOrderCreationAndMatchingPersistence() {
		// Given: Create first order (maker)
		CreateOrderRequest makerRequest = new CreateOrderRequest();
		makerRequest.setUserId(100L);
		makerRequest.setMarketId(1L);
		makerRequest.setOutcome(Outcome.YES);
		makerRequest.setSide(Side.SELL);
		makerRequest.setType(OrderType.LIMIT);
		makerRequest.setPrice(new BigDecimal("100.00"));
		makerRequest.setQuantity(new BigDecimal("10.00"));
		makerRequest.setTimeInForce(TimeInForce.GTC);
		makerRequest.setIdempotencyKey("maker-order-1");

		CreateOrderResponse makerResponse = orderService.createOrder(makerRequest);
		assertThat(makerResponse.getOrderId()).isNotNull();
		assertThat(makerResponse.getStatus()).isEqualTo(OrderStatus.OPEN);
		outboxEventDispatcher.dispatch();

		// When: Create second order (taker) that matches
		CreateOrderRequest takerRequest = new CreateOrderRequest();
		takerRequest.setUserId(200L);
		takerRequest.setMarketId(1L);
		takerRequest.setOutcome(Outcome.YES);
		takerRequest.setSide(Side.BUY);
		takerRequest.setType(OrderType.LIMIT);
		takerRequest.setPrice(new BigDecimal("100.00"));
		takerRequest.setQuantity(new BigDecimal("5.00"));
		takerRequest.setTimeInForce(TimeInForce.GTC);
		takerRequest.setIdempotencyKey("taker-order-1");

		CreateOrderResponse takerResponse = orderService.createOrder(takerRequest);
		assertThat(takerResponse.getOrderId()).isNotNull();
		assertThat(takerResponse.getStatus()).isEqualTo(OrderStatus.OPEN);

		// Then: Wait for matching engine callback to process and verify
		final Long takerOrderId = takerResponse.getOrderId();
		final Long makerOrderId = makerResponse.getOrderId();

		outboxEventDispatcher.dispatch();
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order takerOrder = orderMapper.selectById(takerOrderId);
			assertThat(takerOrder).isNotNull();
			assertThat(takerOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
			assertThat(takerOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);

			Order makerOrder = orderMapper.selectById(makerOrderId);
			assertThat(makerOrder).isNotNull();
			assertThat(makerOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
			assertThat(makerOrder.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("5.00"));

			// Verify fills persisted
			List<OrderFill> fills = orderFillMapper
				.selectList(new LambdaQueryWrapper<OrderFill>().eq(OrderFill::getTakerOrderId, takerOrderId));
			assertThat(fills).hasSize(1);
			assertThat(fills.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("5.00"));
		});
	}

	/**
	 * I-02: Test partial fill
	 */
	@Test
	@DisplayName("I-02: Partial fill with correct quantity")
	void testPartialFill() {
		// Given: Create maker order with only 3.00 available
		CreateOrderRequest makerRequest = new CreateOrderRequest();
		makerRequest.setUserId(100L);
		makerRequest.setMarketId(1L);
		makerRequest.setOutcome(Outcome.YES);
		makerRequest.setSide(Side.SELL);
		makerRequest.setType(OrderType.LIMIT);
		makerRequest.setPrice(new BigDecimal("100.00"));
		makerRequest.setQuantity(new BigDecimal("3.00"));
		makerRequest.setTimeInForce(TimeInForce.GTC);
		makerRequest.setIdempotencyKey("maker-order-2");

		CreateOrderResponse makerResponse = orderService.createOrder(makerRequest);
		assertThat(makerResponse.getOrderId()).isNotNull();
		assertThat(makerResponse.getStatus()).isEqualTo(OrderStatus.OPEN);
		outboxEventDispatcher.dispatch();

		// When: Create taker order wanting 10.00 (more than available)
		CreateOrderRequest takerRequest = new CreateOrderRequest();
		takerRequest.setUserId(200L);
		takerRequest.setMarketId(1L);
		takerRequest.setOutcome(Outcome.YES);
		takerRequest.setSide(Side.BUY);
		takerRequest.setType(OrderType.LIMIT);
		takerRequest.setPrice(new BigDecimal("100.00"));
		takerRequest.setQuantity(new BigDecimal("10.00"));
		takerRequest.setTimeInForce(TimeInForce.GTC);
		takerRequest.setIdempotencyKey("taker-order-2");

		CreateOrderResponse takerResponse = orderService.createOrder(takerRequest);
		assertThat(takerResponse.getOrderId()).isNotNull();
		assertThat(takerResponse.getStatus()).isEqualTo(OrderStatus.OPEN);

		// Then: Wait for callback and verify partial fill
		final Long takerOrderId = takerResponse.getOrderId();
		final Long makerOrderId = makerResponse.getOrderId();

		outboxEventDispatcher.dispatch();
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order takerOrder = orderMapper.selectById(takerOrderId);
			assertThat(takerOrder.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
			assertThat(takerOrder.getRemainingQuantity()).isEqualByComparingTo(new BigDecimal("7.00"));

			Order makerOrder = orderMapper.selectById(makerOrderId);
			assertThat(makerOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
			assertThat(makerOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
		});
	}

	/**
	 * I-03: Test complete fill
	 */
	@Test
	@DisplayName("I-03: Complete fill with FILLED status")
	void testCompleteFill() {
		// Given: Create maker order
		CreateOrderRequest makerRequest = new CreateOrderRequest();
		makerRequest.setUserId(100L);
		makerRequest.setMarketId(1L);
		makerRequest.setOutcome(Outcome.YES);
		makerRequest.setSide(Side.SELL);
		makerRequest.setType(OrderType.LIMIT);
		makerRequest.setPrice(new BigDecimal("100.00"));
		makerRequest.setQuantity(new BigDecimal("10.00"));
		makerRequest.setTimeInForce(TimeInForce.GTC);
		makerRequest.setIdempotencyKey("maker-order-3");

		CreateOrderResponse makerResponse = orderService.createOrder(makerRequest);
		assertThat(makerResponse.getOrderId()).isNotNull();
		assertThat(makerResponse.getStatus()).isEqualTo(OrderStatus.OPEN);
		outboxEventDispatcher.dispatch();

		// When: Create taker order with exact same quantity
		CreateOrderRequest takerRequest = new CreateOrderRequest();
		takerRequest.setUserId(200L);
		takerRequest.setMarketId(1L);
		takerRequest.setOutcome(Outcome.YES);
		takerRequest.setSide(Side.BUY);
		takerRequest.setType(OrderType.LIMIT);
		takerRequest.setPrice(new BigDecimal("100.00"));
		takerRequest.setQuantity(new BigDecimal("10.00"));
		takerRequest.setTimeInForce(TimeInForce.GTC);
		takerRequest.setIdempotencyKey("taker-order-3");

		CreateOrderResponse takerResponse = orderService.createOrder(takerRequest);
		assertThat(takerResponse.getOrderId()).isNotNull();
		assertThat(takerResponse.getStatus()).isEqualTo(OrderStatus.OPEN);

		// Then: Wait for callback and verify both orders are FILLED
		final Long takerOrderId = takerResponse.getOrderId();
		final Long makerOrderId = makerResponse.getOrderId();

		outboxEventDispatcher.dispatch();
		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			Order takerOrder = orderMapper.selectById(takerOrderId);
			assertThat(takerOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
			assertThat(takerOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);

			Order makerOrder = orderMapper.selectById(makerOrderId);
			assertThat(makerOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
			assertThat(makerOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
		});
	}

}
