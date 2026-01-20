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

import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.cmd.CommandResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for OrderEventHandler
 *
 * @author lengleng
 * @date 2025/01/20
 */
@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private ExchangeApi exchangeApi;

	@Mock
	private MatchingEngineSymbolService matchingEngineSymbolService;

	@InjectMocks
	private OrderEventHandler orderEventHandler;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	// ========================================
	// handleOrderCreated Tests
	// ========================================

	@Test
	void testHandleOrderCreated_Success() {
		// Given
		Long orderId = 1001L;
		String eventId = "event-1";
		String aggregateId = "order-1001";
		String payloadJson = "{\"orderId\": 1001}";

		DomainEventEnvelope event = new DomainEventEnvelope(eventId, "order", "Order", aggregateId, "OrderCreated",
				Instant.now(), null, payloadJson);

		Order order = createOrder(orderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10));

		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(exchangeApi.submitCommandAsync(any()))
			.thenReturn(CompletableFuture.completedFuture(CommandResultCode.SUCCESS));
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		orderEventHandler.handleOrderCreated(event);

		// Then
		verify(matchingEngineSymbolService).ensureSymbol(order.getMarketId().intValue());
		verify(exchangeApi).submitCommandAsync(any());
		verify(orderMapper).updateById(orderCaptor.capture());

		Order updatedOrder = orderCaptor.getValue();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.MATCHING);
	}

	@Test
	void testHandleOrderCreated_OrderRejectedByEngine() {
		// Given
		Long orderId = 1001L;
		String payloadJson = "{\"orderId\": 1001}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-1001", "OrderCreated",
				Instant.now(), null, payloadJson);

		Order order = createOrder(orderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10));

		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(exchangeApi.submitCommandAsync(any()))
			.thenReturn(CompletableFuture.completedFuture(CommandResultCode.MATCHING_UNSUPPORTED_COMMAND));
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		orderEventHandler.handleOrderCreated(event);

		// Then
		verify(orderMapper).updateById(orderCaptor.capture());
		Order updatedOrder = orderCaptor.getValue();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.REJECTED);
		assertThat(updatedOrder.getRejectReason()).contains("Matching engine rejected");
	}

	@Test
	void testHandleOrderCreated_OrderNotFound() {
		// Given
		String payloadJson = "{\"orderId\": 9999}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-9999", "OrderCreated",
				Instant.now(), null, payloadJson);

		when(orderMapper.selectById(9999L)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> orderEventHandler.handleOrderCreated(event)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderCreated event");
	}

	@Test
	void testHandleOrderCreated_OrderNotInOpenState() {
		// Given
		Long orderId = 1001L;
		String payloadJson = "{\"orderId\": 1001}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-1001", "OrderCreated",
				Instant.now(), null, payloadJson);

		Order order = createOrder(orderId, OrderStatus.MATCHING, BigDecimal.valueOf(100), BigDecimal.valueOf(10));

		when(orderMapper.selectById(orderId)).thenReturn(order);

		// When
		orderEventHandler.handleOrderCreated(event);

		// Then - should skip processing
		verify(exchangeApi, never()).submitCommandAsync(any());
		verify(orderMapper, never()).updateById(any(Order.class));
	}

	@Test
	void testHandleOrderCreated_InvalidPayload() {
		// Given
		String payloadJson = "{\"invalidKey\": \"value\"}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-1001", "OrderCreated",
				Instant.now(), null, payloadJson);

		// When & Then
		assertThatThrownBy(() -> orderEventHandler.handleOrderCreated(event)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderCreated event");
	}

	// ========================================
	// handleOrderCancelRequested Tests
	// ========================================

	@Test
	void testHandleOrderCancelRequested_Success() {
		// Given
		Long orderId = 1001L;
		Long userId = 123L;
		Long marketId = 1L;
		String payloadJson = "{\"orderId\": 1001, \"userId\": 123, \"marketId\": 1}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-1001",
				"OrderCancelRequested", Instant.now(), null, payloadJson);

		Order order = createOrder(orderId, OrderStatus.CANCEL_REQUESTED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		order.setMarketId(marketId);

		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(exchangeApi.submitCommandAsync(any()))
			.thenReturn(CompletableFuture.completedFuture(CommandResultCode.SUCCESS));

		// When
		orderEventHandler.handleOrderCancelRequested(event);

		// Then
		verify(exchangeApi).submitCommandAsync(any());
	}

	@Test
	void testHandleOrderCancelRequested_OrderNotInCancelRequestedState() {
		// Given
		Long orderId = 1001L;
		String payloadJson = "{\"orderId\": 1001, \"userId\": 123, \"marketId\": 1}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-1001",
				"OrderCancelRequested", Instant.now(), null, payloadJson);

		Order order = createOrder(orderId, OrderStatus.FILLED, BigDecimal.valueOf(100), BigDecimal.ZERO);

		when(orderMapper.selectById(orderId)).thenReturn(order);

		// When
		orderEventHandler.handleOrderCancelRequested(event);

		// Then - should skip processing
		verify(exchangeApi, never()).submitCommandAsync(any());
	}

	@Test
	void testHandleOrderCancelRequested_InvalidPayload() {
		// Given
		String payloadJson = "{\"orderId\": 1001}"; // Missing userId and marketId
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-1001",
				"OrderCancelRequested", Instant.now(), null, payloadJson);

		// When & Then
		assertThatThrownBy(() -> orderEventHandler.handleOrderCancelRequested(event))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderCancelRequested event");
	}

	@Test
	void testHandleOrderCancelRequested_OrderNotFound() {
		// Given
		String payloadJson = "{\"orderId\": 9999, \"userId\": 123, \"marketId\": 1}";
		DomainEventEnvelope event = new DomainEventEnvelope("event-1", "order", "Order", "order-9999",
				"OrderCancelRequested", Instant.now(), null, payloadJson);

		when(orderMapper.selectById(9999L)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> orderEventHandler.handleOrderCancelRequested(event))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderCancelRequested event");
	}

	// ========================================
	// Helper Methods
	// ========================================

	private Order createOrder(Long orderId, OrderStatus status, BigDecimal price, BigDecimal remainingQuantity) {
		Order order = new Order();
		order.setOrderId(orderId);
		order.setUserId(123L);
		order.setMarketId(1L);
		order.setSide(com.pig4cloud.pig.order.api.enums.Side.BUY);
		order.setOrderType(com.pig4cloud.pig.order.api.enums.OrderType.LIMIT);
		order.setTimeInForce(com.pig4cloud.pig.order.api.enums.TimeInForce.GTC);
		order.setPrice(price);
		order.setQuantity(remainingQuantity);
		order.setRemainingQuantity(remainingQuantity);
		order.setStatus(status);
		return order;
	}

}
