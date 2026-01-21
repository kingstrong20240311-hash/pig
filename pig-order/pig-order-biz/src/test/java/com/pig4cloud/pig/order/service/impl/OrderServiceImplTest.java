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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pig4cloud.pig.common.core.util.R;
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
import com.pig4cloud.pig.order.match.MatchingEngineSymbolService;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.cmd.CommandResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl
 *
 * @author lengleng
 * @date 2025/01/19
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderCancelMapper orderCancelMapper;

	@Mock
	private DomainEventPublisher domainEventPublisher;

	@Mock
	private OutboxEventService outboxEventService;

	@Mock
	private VaultService vaultService;

	@Mock
	private ExchangeApi exchangeApi;

	@Mock
	private MarketService marketService;

	@Mock
	private MatchingEngineSymbolService matchingEngineSymbolService;

	@InjectMocks
	private OrderServiceImpl orderService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(orderService, "nodeId", 0L);
	}

	/**
	 * U-04: Test successful order creation flow
	 */
	@Test
	@DisplayName("U-04: Successful order creation flow")
	void testCreateOrderSuccess() {
		// Given
		CreateOrderRequest request = new CreateOrderRequest();
		request.setUserId(100L);
		request.setMarketId(1L);
		request.setSide(Side.BUY);
		request.setType(OrderType.LIMIT);
		request.setPrice(new BigDecimal("100.00"));
		request.setQuantity(new BigDecimal("10.00"));
		request.setTimeInForce(TimeInForce.GTC);
		request.setIdempotencyKey("test-idempotency-key-1");

		// Mock: no existing order
		when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		doNothing().when(marketService).assertMarketActive(anyLong());

		// Mock: freeze success
		FreezeResponse freezeResponse = new FreezeResponse();
		freezeResponse.setFreezeId(1000L);
		when(vaultService.createFreeze(any())).thenReturn(R.ok(freezeResponse));

		// Mock: insert order
		when(orderMapper.insert(any(Order.class))).thenReturn(1);

		// Mock: domain event publisher
		doNothing().when(domainEventPublisher).publish(any());

		// When
		CreateOrderResponse response = orderService.createOrder(request);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OrderStatus.OPEN);
		assertThat(response.getRemainingQuantity()).isEqualTo(new BigDecimal("10.00"));

		// Verify interactions
		verify(orderMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
		verify(vaultService, times(1)).createFreeze(any());
		verify(orderMapper, times(1)).insert(any(Order.class));
		verify(domainEventPublisher, times(1)).publish(any());
	}

	/**
	 * U-05: Test order creation failure due to insufficient balance
	 */
	@Test
	@DisplayName("U-05: Order creation failure due to insufficient balance")
	void testCreateOrderFailureInsufficientBalance() {
		// Given
		CreateOrderRequest request = new CreateOrderRequest();
		request.setUserId(100L);
		request.setMarketId(1L);
		request.setSide(Side.BUY);
		request.setType(OrderType.LIMIT);
		request.setPrice(new BigDecimal("100.00"));
		request.setQuantity(new BigDecimal("10.00"));
		request.setTimeInForce(TimeInForce.GTC);
		request.setIdempotencyKey("test-idempotency-key-2");

		// Mock: no existing order
		when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		doNothing().when(marketService).assertMarketActive(anyLong());

		// Mock: freeze failure (insufficient balance)
		when(vaultService.createFreeze(any())).thenReturn(R.failed("Insufficient balance"));

		// When
		CreateOrderResponse response = orderService.createOrder(request);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(OrderStatus.REJECTED);
		assertThat(response.getRejectReason()).contains("Insufficient balance");

		// Verify interactions
		verify(orderMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
		verify(vaultService, times(1)).createFreeze(any());
		verify(orderMapper, never()).insert(any(Order.class));
		verify(domainEventPublisher, never()).publish(any());
	}

	/**
	 * U-06: Test successful order cancellation
	 */
	@Test
	@DisplayName("U-06: Successful order cancellation")
	void testCancelOrderSuccess() {
		// Given
		CancelOrderRequest request = new CancelOrderRequest();
		request.setOrderId(1000L);
		request.setReason("User requested");
		request.setIdempotencyKey("cancel-idempotency-key-1");

		// Mock: no existing cancel record
		when(orderCancelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

		// Mock: order exists and is cancellable
		Order order = new Order();
		order.setOrderId(1000L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setStatus(OrderStatus.MATCHING);
		order.setRemainingQuantity(new BigDecimal("10.00"));
		when(orderMapper.selectById(1000L)).thenReturn(order);

		// Mock: insert cancel record
		when(orderCancelMapper.insert(any(OrderCancel.class))).thenReturn(1);

		// Mock: update order status
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// Mock: domain event publisher
		doNothing().when(domainEventPublisher).publish(any());

		// When
		CancelOrderResponse response = orderService.cancelOrder(request);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isEqualTo(1000L);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCEL_REQUESTED);

		// Verify interactions
		verify(orderCancelMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
		verify(orderMapper, times(1)).selectById(1000L);
		verify(orderCancelMapper, times(1)).insert(any(OrderCancel.class));
		verify(orderMapper, times(1)).updateById(any(Order.class));
		verify(domainEventPublisher, times(1)).publish(any());
	}

	/**
	 * U-07: Test order cancellation when order is not cancellable
	 */
	@Test
	@DisplayName("U-07: Order cancellation when order is not cancellable")
	void testCancelOrderNotCancellable() {
		// Given
		CancelOrderRequest request = new CancelOrderRequest();
		request.setOrderId(1000L);
		request.setReason("User requested");
		request.setIdempotencyKey("cancel-idempotency-key-2");

		// Mock: no existing cancel record
		when(orderCancelMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

		// Mock: order exists but is already filled (not cancellable)
		Order order = new Order();
		order.setOrderId(1000L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setStatus(OrderStatus.FILLED);
		order.setRemainingQuantity(BigDecimal.ZERO);
		when(orderMapper.selectById(1000L)).thenReturn(order);

		// When
		CancelOrderResponse response = orderService.cancelOrder(request);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isEqualTo(1000L);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.FILLED);

		// Verify interactions - should not insert cancel record or update order
		verify(orderCancelMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
		verify(orderMapper, times(1)).selectById(1000L);
		verify(orderCancelMapper, never()).insert(any(OrderCancel.class));
		verify(orderMapper, never()).updateById(any(Order.class));
		verify(domainEventPublisher, never()).publish(any());
	}

}
