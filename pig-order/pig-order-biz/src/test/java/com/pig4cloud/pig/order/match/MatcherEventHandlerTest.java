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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pig4cloud.pig.order.api.dto.CommitMatchRequest;
import com.pig4cloud.pig.order.api.dto.CommitMatchResponse;
import com.pig4cloud.pig.order.api.dto.FillDTO;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderFill;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.common.error.service.ErrorRecordService;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import exchange.core2.core.IEventsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for MatcherEventHandler
 *
 * @author lengleng
 * @date 2025/01/20
 */
@ExtendWith(MockitoExtension.class)
class MatcherEventHandlerTest {

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderFillMapper orderFillMapper;

	@Mock
	private DomainEventPublisher domainEventPublisher;

	@Mock
	private ErrorRecordService errorRecordService;

	@InjectMocks
	private MatcherEventHandler matcherEventHandler;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	@Captor
	private ArgumentCaptor<OrderFill> orderFillCaptor;

	@Captor
	private ArgumentCaptor<DomainEventEnvelope> eventCaptor;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(matcherEventHandler, "nodeId", 0L);
	}

	// ========================================
	// handleTradeEvent Tests
	// ========================================

	@Test
	void testHandleTradeEvent_Success() {
		// Given
		long takerOrderId = 1001L;
		long makerOrderId = 1002L;
		long timestamp = System.currentTimeMillis();

		IEventsHandler.Trade trade = new IEventsHandler.Trade(makerOrderId, // makerOrderId
				123L, // makerUid
				false, // makerOrderCompleted
				10000L, // price (100.00)
				500L // volume (5.00)
		);

		List<IEventsHandler.Trade> trades = new ArrayList<>();
		trades.add(trade);

		IEventsHandler.TradeEvent tradeEvent = new IEventsHandler.TradeEvent(1, // symbol
				500L, // totalVolume
				takerOrderId, // takerOrderId
				123L, // takerUid
				exchange.core2.core.common.OrderAction.BID, // takerAction
				false, // takeOrderCompleted
				timestamp, // timestamp
				trades // trades
		);

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		matcherEventHandler.handleTradeEvent(tradeEvent);

		// Then
		verify(orderFillMapper).insert(any(OrderFill.class));
		verify(orderMapper, atLeastOnce()).updateById(any(Order.class));
		verify(domainEventPublisher).publish(any(DomainEventEnvelope.class));
	}

	@Test
	void testHandleTradeEvent_MultipleTrades() {
		// Given
		long takerOrderId = 1001L;
		long makerOrderId1 = 1002L;
		long makerOrderId2 = 1003L;

		IEventsHandler.Trade trade1 = new IEventsHandler.Trade(makerOrderId1, // makerOrderId
				123L, // makerUid
				false, // makerOrderCompleted
				10000L, // price
				300L // volume
		);

		IEventsHandler.Trade trade2 = new IEventsHandler.Trade(makerOrderId2, // makerOrderId
				124L, // makerUid
				false, // makerOrderCompleted
				10000L, // price
				200L // volume
		);

		List<IEventsHandler.Trade> trades = new ArrayList<>();
		trades.add(trade1);
		trades.add(trade2);

		IEventsHandler.TradeEvent tradeEvent = new IEventsHandler.TradeEvent(1, // symbol
				500L, // totalVolume
				takerOrderId, // takerOrderId
				123L, // takerUid
				exchange.core2.core.common.OrderAction.BID, // takerAction
				false, // takeOrderCompleted
				System.currentTimeMillis(), // timestamp
				trades // trades
		);

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder1 = createOrder(makerOrderId1, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder2 = createOrder(makerOrderId2, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId1)).thenReturn(makerOrder1);
		when(orderMapper.selectById(makerOrderId2)).thenReturn(makerOrder2);
		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		matcherEventHandler.handleTradeEvent(tradeEvent);

		// Then
		verify(orderFillMapper, times(2)).insert(any(OrderFill.class));
		verify(orderMapper, atLeastOnce()).updateById(any(Order.class));
	}

	// ========================================
	// handleReduceEvent Tests
	// ========================================

	@Test
	void testHandleReduceEvent_OrderCompleted() {
		// Given
		long orderId = 1001L;
		IEventsHandler.ReduceEvent reduceEvent = new IEventsHandler.ReduceEvent(1, // symbol
				1000L, // reducedVolume
				true, // orderCompleted
				10000L, // price
				orderId, // orderId
				123L, // uid
				System.currentTimeMillis() // timestamp
		);

		Order order = createOrder(orderId, OrderStatus.CANCEL_REQUESTED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		matcherEventHandler.handleReduceEvent(reduceEvent);

		// Then - status CANCELLED; remainingQuantity preserved (amount not filled when
		// cancelled)
		verify(orderMapper).updateById(orderCaptor.capture());
		Order updatedOrder = orderCaptor.getValue();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(updatedOrder.getRemainingQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
	}

	@Test
	void testHandleReduceEvent_OrderNotFound() {
		// Given
		IEventsHandler.ReduceEvent reduceEvent = new IEventsHandler.ReduceEvent(1, // symbol
				1000L, // reducedVolume
				true, // orderCompleted
				10000L, // price
				9999L, // orderId
				123L, // uid
				System.currentTimeMillis() // timestamp
		);

		when(orderMapper.selectById(9999L)).thenReturn(null);

		// When
		matcherEventHandler.handleReduceEvent(reduceEvent);

		// Then - should not throw, just log warning
		verify(orderMapper, never()).updateById(any(Order.class));
	}

	// ========================================
	// handleRejectEvent Tests
	// ========================================

	@Test
	void testHandleRejectEvent_Success() {
		// Given
		long orderId = 1001L;
		IEventsHandler.RejectEvent rejectEvent = new IEventsHandler.RejectEvent(1, // symbol
				1000L, // rejectedVolume
				10000L, // price
				orderId, // orderId
				123L, // uid
				System.currentTimeMillis() // timestamp
		);

		Order order = createOrder(orderId, OrderStatus.MATCHING, BigDecimal.valueOf(100), BigDecimal.valueOf(10));

		when(orderMapper.selectById(orderId)).thenReturn(order);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		matcherEventHandler.handleRejectEvent(rejectEvent);

		// Then
		verify(orderMapper).updateById(orderCaptor.capture());
		Order updatedOrder = orderCaptor.getValue();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.REJECTED);
		assertThat(updatedOrder.getRejectReason()).contains("IOC order could not be filled");
	}

	@Test
	void testHandleRejectEvent_OrderNotFound() {
		// Given
		IEventsHandler.RejectEvent rejectEvent = new IEventsHandler.RejectEvent(1, // symbol
				1000L, // rejectedVolume
				10000L, // price
				9999L, // orderId
				123L, // uid
				System.currentTimeMillis() // timestamp
		);

		when(orderMapper.selectById(9999L)).thenReturn(null);

		// When
		matcherEventHandler.handleRejectEvent(rejectEvent);

		// Then - should not throw, just log warning
		verify(orderMapper, never()).updateById(any(Order.class));
	}

	// ========================================
	// handleCommandResult Tests
	// ========================================

	@Test
	void testHandleCommandResult() {
		// Given
		IEventsHandler.ApiCommandResult result = new IEventsHandler.ApiCommandResult(null, // command
				exchange.core2.core.common.cmd.CommandResultCode.SUCCESS, // resultCode
				12345L // seq
		);

		// When
		matcherEventHandler.handleCommandResult(result);

		// Then - should just log, no other actions
		verifyNoInteractions(orderMapper, orderFillMapper);
	}

	// ========================================
	// commitMatch Tests
	// ========================================

	@Test
	void testCommitMatch_Success() {
		// Given
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		CommitMatchResponse response = matcherEventHandler.commitMatch(request);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getMatchId()).isEqualTo(matchId);
		assertThat(response.getOrderStates()).hasSize(2);
		assertThat(response.getSettlementRequired()).isTrue();

		verify(orderFillMapper).insert(any(OrderFill.class));
		verify(orderMapper, times(2)).updateById(any(Order.class)); // taker and maker
		verify(domainEventPublisher).publish(any(DomainEventEnvelope.class));
	}

	@Test
	void testCommitMatch_PartialFill() {
		// Given
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(3)); // Partial fill
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		CommitMatchResponse response = matcherEventHandler.commitMatch(request);

		// Then
		verify(orderMapper, times(2)).updateById(orderCaptor.capture());
		List<Order> updatedOrders = orderCaptor.getAllValues();

		// Check that both orders are PARTIALLY_FILLED
		assertThat(updatedOrders).extracting(Order::getStatus)
			.containsOnly(OrderStatus.PARTIALLY_FILLED, OrderStatus.PARTIALLY_FILLED);
	}

	@Test
	void testCommitMatch_FullFill() {
		// Given
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(10)); // Full fill
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When
		matcherEventHandler.commitMatch(request);

		// Then
		verify(orderMapper, times(2)).updateById(orderCaptor.capture());
		List<Order> updatedOrders = orderCaptor.getAllValues();

		// Check that both orders are FILLED
		assertThat(updatedOrders).extracting(Order::getStatus).containsOnly(OrderStatus.FILLED, OrderStatus.FILLED);
		assertThat(updatedOrders).extracting(Order::getRemainingQuantity)
			.containsOnly(BigDecimal.ZERO, BigDecimal.ZERO);
	}

	@Test
	void testCommitMatch_Idempotency() {
		// Given
		Long takerOrderId = 1001L;
		String matchId = "match-123";

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(new ArrayList<>());
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.FILLED, BigDecimal.valueOf(100), BigDecimal.ZERO);

		// Simulate that fills already exist for this matchId
		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);

		// When
		CommitMatchResponse response = matcherEventHandler.commitMatch(request);

		// Then - should return existing result without processing
		assertThat(response).isNotNull();
		assertThat(response.getMatchId()).isEqualTo(matchId);
		verify(orderFillMapper, never()).insert(any(OrderFill.class));
		verify(orderMapper, never()).updateById(any(Order.class));
		verify(domainEventPublisher, never()).publish(any(DomainEventEnvelope.class));
	}

	@Test
	void testCommitMatch_MissingIdempotencyKey() {
		// Given
		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId("match-123");
		request.setTakerOrderId(1001L);
		request.setFills(new ArrayList<>());
		request.setIdempotencyKey(null); // Missing

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Idempotency key is required");
	}

	@Test
	void testCommitMatch_TakerOrderNotFound() {
		// Given
		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId("match-123");
		request.setTakerOrderId(9999L);
		request.setFills(new ArrayList<>());
		request.setIdempotencyKey("idempotency-123");

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(9999L)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Taker order not found");
	}

	@Test
	void testCommitMatch_MakerOrderNotFound() {
		// Given
		Long takerOrderId = 1001L;
		Long makerOrderId = 9999L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(null);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Maker order not found");
	}

	@Test
	void testCommitMatch_NegativeMakerRemaining() {
		// Given
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(15)); // More than remaining
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(20));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10)); // Only 10 remaining

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Maker order remaining quantity cannot be negative");
	}

	@Test
	void testCommitMatch_NegativeTakerRemaining() {
		// Given
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(15)); // More than taker's remaining
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10)); // Only 10 remaining
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(20));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Taker order remaining quantity cannot be negative");
	}

	@Test
	void testCommitMatch_TakerOrderInOpenState() {
		// Given - Test eventual consistency: callback arrives before submission
		// completes
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.OPEN, BigDecimal.valueOf(100), BigDecimal.valueOf(10)); // Still
																															// in
																															// OPEN
																															// state
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When - Should proceed without error
		CommitMatchResponse response = matcherEventHandler.commitMatch(request);

		// Then
		assertThat(response).isNotNull();
		verify(orderFillMapper).insert(any(OrderFill.class));
		verify(orderMapper, times(2)).updateById(any(Order.class));
	}

	@Test
	void testCommitMatch_TakerOrderWithCancelRequestedStatus() {
		// Given - CANCEL_REQUESTED orders should still be matchable
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.CANCEL_REQUESTED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When - Should proceed without error
		CommitMatchResponse response = matcherEventHandler.commitMatch(request);

		// Then
		assertThat(response).isNotNull();
		verify(orderFillMapper).insert(any(OrderFill.class));
		verify(orderMapper, times(2)).updateById(any(Order.class));
	}

	@Test
	void testCommitMatch_MakerOrderWithCancelRequestedStatus() {
		// Given - CANCEL_REQUESTED maker orders should still be matchable
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.CANCEL_REQUESTED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);
		when(orderFillMapper.insert(any(OrderFill.class))).thenReturn(1);
		when(orderMapper.updateById(any(Order.class))).thenReturn(1);

		// When - Should proceed without error
		CommitMatchResponse response = matcherEventHandler.commitMatch(request);

		// Then
		assertThat(response).isNotNull();
		verify(orderFillMapper).insert(any(OrderFill.class));
		verify(orderMapper, times(2)).updateById(any(Order.class));
	}

	@Test
	void testCommitMatch_TakerOrderWithFilledStatus() {
		// Given - FILLED orders should NOT be matchable
		Long takerOrderId = 1001L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(1002L);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.FILLED, BigDecimal.valueOf(100), BigDecimal.ZERO);

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Taker order cannot be matched in current state")
			.hasMessageContaining("FILLED");
	}

	@Test
	void testCommitMatch_TakerOrderWithCancelledStatus() {
		// Given - CANCELLED orders should NOT be matchable
		Long takerOrderId = 1001L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(1002L);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.CANCELLED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(5));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Taker order cannot be matched in current state")
			.hasMessageContaining("CANCELLED");
	}

	@Test
	void testCommitMatch_TakerOrderWithExpiredStatus() {
		// Given - EXPIRED orders should NOT be matchable
		Long takerOrderId = 1001L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(1002L);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.EXPIRED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(5));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Taker order cannot be matched in current state")
			.hasMessageContaining("EXPIRED");
	}

	@Test
	void testCommitMatch_TakerOrderWithRejectedStatus() {
		// Given - REJECTED orders should NOT be matchable
		Long takerOrderId = 1001L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(1002L);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.REJECTED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(5));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Taker order cannot be matched in current state")
			.hasMessageContaining("REJECTED");
	}

	@Test
	void testCommitMatch_TakerOrderWithFailedStatus() {
		// Given - FAILED orders should NOT be matchable
		Long takerOrderId = 1001L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(1002L);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.FAILED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(5));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Taker order cannot be matched in current state")
			.hasMessageContaining("FAILED");
	}

	@Test
	void testCommitMatch_MakerOrderWithFilledStatus() {
		// Given - FILLED maker orders should NOT be matchable
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.FILLED, BigDecimal.valueOf(100), BigDecimal.ZERO);

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Maker order cannot be matched in current state")
			.hasMessageContaining("FILLED");
	}

	@Test
	void testCommitMatch_MakerOrderWithCancelledStatus() {
		// Given - CANCELLED maker orders should NOT be matchable
		Long takerOrderId = 1001L;
		Long makerOrderId = 1002L;
		String matchId = "match-123";

		FillDTO fillDTO = new FillDTO();
		fillDTO.setMakerOrderId(makerOrderId);
		fillDTO.setPrice(BigDecimal.valueOf(100));
		fillDTO.setQuantity(BigDecimal.valueOf(5));
		fillDTO.setFee(BigDecimal.ZERO);

		List<FillDTO> fills = new ArrayList<>();
		fills.add(fillDTO);

		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey("idempotency-123");

		Order takerOrder = createOrder(takerOrderId, OrderStatus.MATCHING, BigDecimal.valueOf(100),
				BigDecimal.valueOf(10));
		Order makerOrder = createOrder(makerOrderId, OrderStatus.CANCELLED, BigDecimal.valueOf(100),
				BigDecimal.valueOf(5));

		when(orderFillMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		when(orderMapper.selectById(takerOrderId)).thenReturn(takerOrder);
		when(orderMapper.selectById(makerOrderId)).thenReturn(makerOrder);

		// When & Then
		assertThatThrownBy(() -> matcherEventHandler.commitMatch(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Maker order cannot be matched in current state")
			.hasMessageContaining("CANCELLED");
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
