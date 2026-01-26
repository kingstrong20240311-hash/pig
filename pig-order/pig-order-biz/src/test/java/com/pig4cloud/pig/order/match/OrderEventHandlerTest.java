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

import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.MarketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCancelRequestedPayload;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCreatedPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
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

	@Mock
	private MatchingEngineProperties matchingEngineProperties;

	@Mock
	private MarketService marketService;

	@Mock
	private DomainEventPublisher domainEventPublisher;

	@Mock
	private ObjectMapper objectMapper;

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
		OrderCreatedPayload payload = new OrderCreatedPayload(orderId, 123L, 1L, "YES", "OPEN");
		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>(eventId, "order", "Order",
				aggregateId, "OrderCreated", System.currentTimeMillis(), null, payload);

		Order order = createOrder(orderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10));
		order.setOutcome(Outcome.YES);
		Market market = createMarket(order.getMarketId(), 101, 102);

		when(marketService.getMarket(order.getMarketId())).thenReturn(market);
		when(matchingEngineProperties.getDefaultAsset()).thenReturn(1);
		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(exchangeApi.submitCommandAsync(any()))
			.thenReturn(CompletableFuture.completedFuture(CommandResultCode.SUCCESS));
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		orderEventHandler.handleOrderCreated(event);

		// Then
		verify(matchingEngineSymbolService).ensureSymbol(101, 101, 1);
		verify(exchangeApi).submitCommandAsync(any());
		verify(orderMapper).updateById(orderCaptor.capture());

		Order updatedOrder = orderCaptor.getValue();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.MATCHING);
	}

	@Test
	void testHandleOrderCreated_BackwardCompatiblePayloadJson() throws Exception {
		// Given
		Long orderId = 1003L;
		String eventId = "event-compat";
		String aggregateId = "order-1003";
		String payloadJson = "{\"orderId\":1003}";

		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>(eventId, "order", "Order",
				aggregateId, "OrderCreated", System.currentTimeMillis(), null, payloadJson);

		OrderCreatedPayload payload = new OrderCreatedPayload(orderId, 123L, 1L, "YES", "OPEN");
		when(objectMapper.readValue(payloadJson, OrderCreatedPayload.class)).thenReturn(payload);

		Order order = createOrder(orderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10));
		order.setOutcome(Outcome.YES);
		Market market = createMarket(order.getMarketId(), 101, 102);
		when(marketService.getMarket(order.getMarketId())).thenReturn(market);
		when(matchingEngineProperties.getDefaultAsset()).thenReturn(1);
		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(exchangeApi.submitCommandAsync(any()))
			.thenReturn(CompletableFuture.completedFuture(CommandResultCode.SUCCESS));
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		orderEventHandler.handleOrderCreated(event);

		// Then
		verify(exchangeApi).submitCommandAsync(any());
		verify(orderMapper).updateById(orderCaptor.capture());
	}

	@Test
	void testHandleOrderCreated_MarketOrderSuccess_NoMatchingTransition() {
		// Given
		Long orderId = 1002L;
		OrderCreatedPayload payload = new OrderCreatedPayload(orderId, 123L, 1L, "YES", "OPEN");
		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>("event-2", "order", "Order",
				"order-1002", "OrderCreated", System.currentTimeMillis(), null, payload);

		Order order = createOrder(orderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10));
		order.setOutcome(Outcome.YES);
		order.setOrderType(com.pig4cloud.pig.order.api.enums.OrderType.MARKET);

		when(marketService.getMarket(order.getMarketId())).thenReturn(createMarket(order.getMarketId(), 101, 102));
		when(matchingEngineProperties.getDefaultAsset()).thenReturn(1);
		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(exchangeApi.submitCommandAsync(any()))
			.thenReturn(CompletableFuture.completedFuture(CommandResultCode.SUCCESS));

		// When
		orderEventHandler.handleOrderCreated(event);

		// Then
		verify(exchangeApi).submitCommandAsync(any());
		verify(orderMapper, never()).updateById(any(Order.class));
	}

	@Test
	void testHandleOrderCreated_OrderRejectedByEngine() {
		// Given
		Long orderId = 1001L;
		OrderCreatedPayload payload = new OrderCreatedPayload(orderId, 123L, 1L, "YES", "OPEN");
		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-1001", "OrderCreated", System.currentTimeMillis(), null, payload);

		Order order = createOrder(orderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10));
		order.setOutcome(Outcome.YES);

		when(marketService.getMarket(order.getMarketId())).thenReturn(createMarket(order.getMarketId(), 101, 102));
		when(matchingEngineProperties.getDefaultAsset()).thenReturn(1);
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
		OrderCreatedPayload payload = new OrderCreatedPayload(9999L, 123L, 1L, "YES", "OPEN");
		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-9999", "OrderCreated", System.currentTimeMillis(), null, payload);

		when(orderMapper.selectById(9999L)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> orderEventHandler.handleOrderCreated(event)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderCreated event");
	}

	@Test
	void testHandleOrderCreated_OrderNotInOpenState() {
		// Given
		Long orderId = 1001L;
		OrderCreatedPayload payload = new OrderCreatedPayload(orderId, 123L, 1L, "YES", "OPEN");
		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-1001", "OrderCreated", System.currentTimeMillis(), null, payload);

		Order order = createOrder(orderId, OrderStatus.MATCHING, BigDecimal.valueOf(100), BigDecimal.valueOf(10));
		order.setOutcome(Outcome.YES);

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
		OrderCreatedPayload payload = new OrderCreatedPayload(null, 123L, 1L, "YES", "OPEN");
		DomainEventEnvelope<OrderCreatedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-1001", "OrderCreated", System.currentTimeMillis(), null, payload);

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
		Long marketId = 1L;
		OrderCancelRequestedPayload payload = new OrderCancelRequestedPayload(orderId, 123L, marketId, "CANCEL_REQUESTED",
				"idem-1");
		DomainEventEnvelope<OrderCancelRequestedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-1001", "OrderCancelRequested", System.currentTimeMillis(), null, payload);

		Order order = createOrder(orderId, OrderStatus.CANCEL_REQUESTED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		order.setOutcome(Outcome.YES);
		order.setMarketId(marketId);

		when(marketService.getMarket(order.getMarketId())).thenReturn(createMarket(order.getMarketId(), 101, 102));
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
		OrderCancelRequestedPayload payload = new OrderCancelRequestedPayload(orderId, 123L, 1L, "FILLED", "idem-2");
		DomainEventEnvelope<OrderCancelRequestedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-1001", "OrderCancelRequested", System.currentTimeMillis(), null, payload);

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
		Long orderId = 1001L;
		OrderCancelRequestedPayload payload = new OrderCancelRequestedPayload(orderId, null, null, null, "idem-3");
		DomainEventEnvelope<OrderCancelRequestedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-1001", "OrderCancelRequested", System.currentTimeMillis(), null, payload);

		// When & Then
		assertThatThrownBy(() -> orderEventHandler.handleOrderCancelRequested(event))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderCancelRequested event");
	}

	@Test
	void testHandleOrderCancelRequested_OrderNotFound() {
		// Given
		OrderCancelRequestedPayload payload = new OrderCancelRequestedPayload(9999L, 123L, 1L, "CANCEL_REQUESTED", "idem-4");
		DomainEventEnvelope<OrderCancelRequestedPayload> event = new DomainEventEnvelope<>("event-1", "order", "Order",
				"order-9999", "OrderCancelRequested", System.currentTimeMillis(), null, payload);

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

	private Market createMarket(Long marketId, Integer symbolIdYes, Integer symbolIdNo) {
		Market market = new Market();
		market.setMarketId(marketId);
		market.setSymbolIdYes(symbolIdYes);
		market.setSymbolIdNo(symbolIdNo);
		return market;
	}

}
