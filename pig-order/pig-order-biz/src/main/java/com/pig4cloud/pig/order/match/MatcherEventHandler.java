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
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.error.service.ErrorRecordService;
import com.pig4cloud.pig.order.api.dto.CommitMatchRequest;
import com.pig4cloud.pig.order.api.dto.CommitMatchResponse;
import com.pig4cloud.pig.order.api.dto.FillDTO;
import com.pig4cloud.pig.order.api.dto.OrderStateDTO;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.entity.OrderFill;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderFillMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.match.dto.FailedReduceEventDTO;
import com.pig4cloud.pig.order.match.dto.FailedRejectEventDTO;
import com.pig4cloud.pig.order.match.dto.FailedTradeEventDTO;
import com.pig4cloud.pig.order.match.dto.MatchCommittedPayload;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCancelPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import exchange.core2.core.IEventsHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matcher Event Handler - Handles matching engine callbacks
 *
 * @author lengleng
 * @date 2025/01/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatcherEventHandler implements OrderMatchService {

	private static final BigDecimal SCALE_FACTOR = BigDecimal.valueOf(100);

	private static final long DATACENTER_ID = 0L;

	private static final String DOMAIN_ORDER = "order";

	private static final String AGG_TYPE_MATCH = "Match";

	private static final String AGG_TYPE_ORDER = "Order";

	private static final String EVENT_ORDER_CANCEL = "OrderCancel";

	private final OrderMapper orderMapper;

	private final OrderFillMapper orderFillMapper;

	private final DomainEventPublisher domainEventPublisher;

	private final ErrorRecordService errorRecordService;

	@Value("${node-id:0}")
	private long nodeId;

	// ========================================
	// Matching Engine Callbacks (IEventsHandler)
	// ========================================

	@Override
	public void handleTradeEvent(IEventsHandler.TradeEvent tradeEvent) {
		log.info("Handling trade event: takerOrderId={}, totalVolume={}, tradesCount={}", tradeEvent.takerOrderId,
				tradeEvent.totalVolume, tradeEvent.trades.size());

		// Generate a unique matchId using timestamp and takerOrderId
		String matchId = String.format("%d-%d", tradeEvent.takerOrderId, tradeEvent.timestamp);

		// Use matchId as idempotency key
		String idempotencyKey = matchId;

		// Convert trades to FillDTOs
		List<FillDTO> fills = new ArrayList<>();
		for (IEventsHandler.Trade trade : tradeEvent.trades) {
			FillDTO fillDTO = new FillDTO();
			fillDTO.setMakerOrderId(trade.makerOrderId);
			// Convert from exchange-core long format to BigDecimal (scaled by 100)
			fillDTO.setPrice(convertLongToDecimal(trade.price));
			fillDTO.setQuantity(convertLongToDecimal(trade.volume));
			fillDTO.setFee(BigDecimal.ZERO); // TODO: Calculate fees if needed
			fills.add(fillDTO);
		}

		// Create commit match request
		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(matchId);
		request.setTakerOrderId(tradeEvent.takerOrderId);
		request.setFills(fills);
		request.setIdempotencyKey(idempotencyKey);

		try {
			// Commit match directly (avoid OrderService dependency)
			commitMatch(request);

			log.info("Trade event processed successfully: matchId={}, takerOrderId={}", matchId,
					tradeEvent.takerOrderId);
		}
		catch (Exception e) {
			log.error("Failed to handle trade event: takerOrderId={}, matchId={}", tradeEvent.takerOrderId, matchId, e);

			// Record error for later compensation/retry using pig-common-error
			FailedTradeEventDTO failedEvent = new FailedTradeEventDTO();
			failedEvent.setMatchId(matchId);
			failedEvent.setTakerOrderId(tradeEvent.takerOrderId);
			failedEvent.setTotalVolume(tradeEvent.totalVolume);
			failedEvent.setTimestamp(tradeEvent.timestamp);
			failedEvent.setFills(fills);
			failedEvent.setIdempotencyKey(idempotencyKey);

			errorRecordService.record("order", // domain
					"order:handleTradeEvent", // handler key
					failedEvent, // failed event data
					e // exception
			);

			log.info("Trade event error recorded for compensation: matchId={}, errorRecorded=true", matchId);
		}
	}

	@Override
	public void handleReduceEvent(IEventsHandler.ReduceEvent reduceEvent) {
		log.info("Handling reduce event: orderId={}, reducedVolume={}, orderCompleted={}", reduceEvent.orderId,
				reduceEvent.reducedVolume, reduceEvent.orderCompleted);

		try {
			// Get the order
			Order order = orderMapper.selectById(reduceEvent.orderId);
			if (order == null) {
				log.warn("Order not found for reduce event: orderId={}", reduceEvent.orderId);
				// Record error for missing order - may need to investigate data
				// inconsistency
				FailedReduceEventDTO failedEvent = new FailedReduceEventDTO();
				failedEvent.setOrderId(reduceEvent.orderId);
				failedEvent.setReducedVolume(reduceEvent.reducedVolume);
				failedEvent.setOrderCompleted(reduceEvent.orderCompleted);
				failedEvent.setTimestamp(reduceEvent.timestamp);

				errorRecordService.record("order", "order:handleReduceEvent", failedEvent,
						new IllegalStateException("Order not found: " + reduceEvent.orderId));
				return;
			}

			// note: No need to check previous status, 
			// because it has been checked before requesting cancel to matching engine 
			// Update order status based on whether it's completed
			if (reduceEvent.orderCompleted) {
				order.setStatus(OrderStatus.CANCELLED);
				order.setRemainingQuantity(BigDecimal.ZERO);
			}
			else {
				// Partial cancel - update remaining quantity
				BigDecimal reducedVolume = convertLongToDecimal(reduceEvent.reducedVolume);
				BigDecimal newRemaining = order.getRemainingQuantity().subtract(reducedVolume);
				order.setRemainingQuantity(newRemaining);
			}

			orderMapper.updateById(order);
			if (reduceEvent.orderCompleted) {
				publishOrderCancelEvent(order, "Order cancelled");
			}
			log.info("Reduce event processed successfully: orderId={}, newStatus={}", reduceEvent.orderId,
					order.getStatus());
		}
		catch (Exception e) {
			log.error("Failed to handle reduce event: orderId={}", reduceEvent.orderId, e);

			// Record error for later compensation/retry using pig-common-error
			FailedReduceEventDTO failedEvent = new FailedReduceEventDTO();
			failedEvent.setOrderId(reduceEvent.orderId);
			failedEvent.setReducedVolume(reduceEvent.reducedVolume);
			failedEvent.setOrderCompleted(reduceEvent.orderCompleted);
			failedEvent.setTimestamp(reduceEvent.timestamp);

			errorRecordService.record("order", "order:handleReduceEvent", failedEvent, e);

			log.info("Reduce event error recorded for compensation: orderId={}, errorRecorded=true",
					reduceEvent.orderId);
		}
	}

	@Override
	public void handleRejectEvent(IEventsHandler.RejectEvent rejectEvent) {
		log.info("Handling reject event: orderId={}, rejectedVolume={}", rejectEvent.orderId,
				rejectEvent.rejectedVolume);

		try {
			// Get the order
			Order order = orderMapper.selectById(rejectEvent.orderId);
			if (order == null) {
				log.warn("Order not found for reject event: orderId={}", rejectEvent.orderId);
				// Record error for missing order - may need to investigate data
				// inconsistency
				FailedRejectEventDTO failedEvent = new FailedRejectEventDTO();
				failedEvent.setOrderId(rejectEvent.orderId);
				failedEvent.setRejectedVolume(rejectEvent.rejectedVolume);
				failedEvent.setTimestamp(rejectEvent.timestamp);

				errorRecordService.record("order", "order:handleRejectEvent", failedEvent,
						new IllegalStateException("Order not found: " + rejectEvent.orderId));
				return;
			}

			// Mark order as rejected (for IOC orders that couldn't be filled)
			order.setStatus(OrderStatus.REJECTED);
			order.setRejectReason("IOC order could not be filled at specified price");

			orderMapper.updateById(order);
			publishOrderCancelEvent(order, order.getRejectReason());
			log.info("Reject event processed successfully: orderId={}", rejectEvent.orderId);
		}
		catch (Exception e) {
			log.error("Failed to handle reject event: orderId={}", rejectEvent.orderId, e);

			// Record error for later compensation/retry using pig-common-error
			FailedRejectEventDTO failedEvent = new FailedRejectEventDTO();
			failedEvent.setOrderId(rejectEvent.orderId);
			failedEvent.setRejectedVolume(rejectEvent.rejectedVolume);
			failedEvent.setTimestamp(rejectEvent.timestamp);

			errorRecordService.record("order", "order:handleRejectEvent", failedEvent, e);

			log.info("Reject event error recorded for compensation: orderId={}, errorRecorded=true",
					rejectEvent.orderId);
		}
	}

	@Override
	public void handleCommandResult(IEventsHandler.ApiCommandResult result) {
		log.debug("Command result: command={}, resultCode={}, seq={}", result.command, result.resultCode, result.seq);

		// Can be used for monitoring/metrics
		// For now, we'll just log it at debug level
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CommitMatchResponse commitMatch(CommitMatchRequest request) {
		// 1. Validate idempotency key must be provided
		String idempotencyKey = request.getIdempotencyKey();
		if (StrUtil.isBlank(idempotencyKey)) {
			throw new IllegalArgumentException("Idempotency key is required");
		}

		// 2. Check idempotency - if fills with this matchId exist, return existing result
		long existingFillCount = orderFillMapper
			.selectCount(Wrappers.<OrderFill>lambdaQuery().eq(OrderFill::getMatchId, request.getMatchId()));

		if (existingFillCount > 0) {
			log.info("Match already committed for matchId: {}, returning existing state", request.getMatchId());
			return buildCommitMatchResponse(request);
		}

		// 3. Get taker order
		Order takerOrder = orderMapper.selectById(request.getTakerOrderId());
		if (takerOrder == null) {
			throw new IllegalArgumentException("Taker order not found: " + request.getTakerOrderId());
		}

		// 3.1 Validate taker order status - only allow matchable states
		if (!isValidMatchingStatus(takerOrder.getStatus())) {
			// invalid states
			log.error(
					"Taker order in invalid state for matching: orderId={}, status={}, matchId={}, allowed states: OPEN, MATCHING, PARTIALLY_FILLED, CANCEL_REQUESTED",
					takerOrder.getOrderId(), takerOrder.getStatus(), request.getMatchId());
			throw new IllegalStateException("Taker order cannot be matched in current state: orderId="
					+ takerOrder.getOrderId() + ", status=" + takerOrder.getStatus());
		}

		// 3.2 Allow matching from OPEN status (callback may arrive before submission
		// completes)
		// This handles eventual consistency - order may still be OPEN if callback arrives
		// early
		if (takerOrder.getStatus() == OrderStatus.OPEN) {
			log.info(
					"Taker order in OPEN state, callback arrived before submission completed: orderId={}, will process match",
					takerOrder.getOrderId());
			// Proceed with matching - this is expected in async model
		}

		// 4. Process each fill
		Map<Long, OrderStateDTO> orderStates = new HashMap<>();
		BigDecimal totalTakerFilled = BigDecimal.ZERO;

		for (FillDTO fillDTO : request.getFills()) {
			// 4.1 Get maker order
			Order makerOrder = orderMapper.selectById(fillDTO.getMakerOrderId());
			if (makerOrder == null) {
				throw new IllegalArgumentException("Maker order not found: " + fillDTO.getMakerOrderId());
			}

			// 4.1.1 Validate maker order status - only allow matchable states
			if (!isValidMatchingStatus(makerOrder.getStatus())) {
				// invalid states
				log.error(
						"Maker order in invalid state for matching: orderId={}, status={}, matchId={}, allowed states: OPEN, MATCHING, PARTIALLY_FILLED, CANCEL_REQUESTED",
						makerOrder.getOrderId(), makerOrder.getStatus(), request.getMatchId());
				throw new IllegalStateException("Maker order cannot be matched in current state: orderId="
						+ makerOrder.getOrderId() + ", status=" + makerOrder.getStatus());
			}

			// 4.2 Insert fill record
			OrderFill fill = new OrderFill();
			fill.setTradeId(IdUtil.getSnowflake(nodeId, DATACENTER_ID).nextId());
			fill.setMatchId(request.getMatchId());
			fill.setTakerOrderId(request.getTakerOrderId());
			fill.setMakerOrderId(fillDTO.getMakerOrderId());
			fill.setPrice(fillDTO.getPrice());
			fill.setQuantity(fillDTO.getQuantity());
			fill.setFee(fillDTO.getFee());
			orderFillMapper.insert(fill);

			// 4.3 Update maker order
			BigDecimal newMakerRemaining = makerOrder.getRemainingQuantity().subtract(fillDTO.getQuantity());

			// Validate remaining quantity: must not be negative
			if (newMakerRemaining.compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalStateException("Maker order remaining quantity cannot be negative: orderId="
						+ makerOrder.getOrderId() + ", remaining=" + makerOrder.getRemainingQuantity() + ", filled="
						+ fillDTO.getQuantity());
			}

			makerOrder.setRemainingQuantity(newMakerRemaining);
			if (newMakerRemaining.compareTo(BigDecimal.ZERO) == 0) {
				// Exactly 0: FILLED
				makerOrder.setStatus(OrderStatus.FILLED);
			}
			else {
				// Greater than 0: PARTIALLY_FILLED
				makerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
			}
			orderMapper.updateById(makerOrder);

			// Track maker order state
			orderStates.put(makerOrder.getOrderId(), buildOrderStateDTO(makerOrder));

			// Accumulate taker filled quantity
			totalTakerFilled = totalTakerFilled.add(fillDTO.getQuantity());
		}

		// 5. Update taker order
		BigDecimal newTakerRemaining = takerOrder.getRemainingQuantity().subtract(totalTakerFilled);

		// Validate remaining quantity: must not be negative
		if (newTakerRemaining.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalStateException(
					"Taker order remaining quantity cannot be negative: orderId=" + takerOrder.getOrderId()
							+ ", remaining=" + takerOrder.getRemainingQuantity() + ", filled=" + totalTakerFilled);
		}

		takerOrder.setRemainingQuantity(newTakerRemaining);
		if (newTakerRemaining.compareTo(BigDecimal.ZERO) == 0) {
			// Exactly 0: FILLED
			takerOrder.setStatus(OrderStatus.FILLED);
		}
		else {
			// Greater than 0: PARTIALLY_FILLED
			takerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
		}
		orderMapper.updateById(takerOrder);

		// Track taker order state
		orderStates.put(takerOrder.getOrderId(), buildOrderStateDTO(takerOrder));

		// 6. Emit MatchCommittedEvent
		publishMatchCommittedEvent(request, orderStates);

		log.info("Match committed: matchId={}, takerOrderId={}, fillCount={}, takerFilled={}", request.getMatchId(),
				request.getTakerOrderId(), request.getFills().size(), totalTakerFilled);

		// 7. Build response
		CommitMatchResponse response = new CommitMatchResponse();
		response.setMatchId(request.getMatchId());
		response.setOrderStates(orderStates);
		response.setSettlementRequired(true);

		return response;
	}

	// ========================================
	// Helper Methods
	// ========================================

	/**
	 * Build OrderStateDTO from Order
	 */
	private OrderStateDTO buildOrderStateDTO(Order order) {
		OrderStateDTO dto = new OrderStateDTO();
		dto.setOrderId(order.getOrderId());
		dto.setStatus(order.getStatus());
		dto.setRemainingQuantity(order.getRemainingQuantity());
		return dto;
	}

	/**
	 * Build CommitMatchResponse (for idempotent replay)
	 */
	private CommitMatchResponse buildCommitMatchResponse(CommitMatchRequest request) {
		// Rebuild response from existing data
		Map<Long, OrderStateDTO> orderStates = new HashMap<>();

		Order takerOrder = orderMapper.selectById(request.getTakerOrderId());
		if (takerOrder != null) {
			orderStates.put(takerOrder.getOrderId(), buildOrderStateDTO(takerOrder));
		}

		for (FillDTO fill : request.getFills()) {
			Order makerOrder = orderMapper.selectById(fill.getMakerOrderId());
			if (makerOrder != null) {
				orderStates.put(makerOrder.getOrderId(), buildOrderStateDTO(makerOrder));
			}
		}

		CommitMatchResponse response = new CommitMatchResponse();
		response.setMatchId(request.getMatchId());
		response.setOrderStates(orderStates);
		response.setSettlementRequired(true);

		return response;
	}

	/**
	 * Publish MatchCommittedEvent via DomainEventPublisher
	 */
	private void publishMatchCommittedEvent(CommitMatchRequest request, Map<Long, OrderStateDTO> orderStates) {
		MatchCommittedPayload payload = new MatchCommittedPayload(request.getMatchId(), request.getTakerOrderId(),
				request.getFills(), orderStates);

		DomainEventEnvelope<MatchCommittedPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_MATCH, // aggregateType
				request.getMatchId(), // aggregateId
				"MatchCommitted", // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	/**
	 * Publish OrderCancel event via DomainEventPublisher
	 */
	private void publishOrderCancelEvent(Order order, String reason) {
		OrderCancelPayload payload = new OrderCancelPayload(order.getOrderId(), order.getUserId(),
				order.getMarketId(), order.getStatus().name(), reason);

		DomainEventEnvelope<OrderCancelPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				EVENT_ORDER_CANCEL, // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	/**
	 * Convert exchange-core long to BigDecimal
	 */
	private static BigDecimal convertLongToDecimal(long value) {
		return BigDecimal.valueOf(value).divide(SCALE_FACTOR);
	}

	/**
	 * Check if order status is valid for matching Only OPEN, MATCHING, PARTIALLY_FILLED,
	 * and CANCEL_REQUESTED orders can be matched
	 * @param status the order status to check
	 * @return true if status allows matching, false otherwise
	 */
	private boolean isValidMatchingStatus(OrderStatus status) {
		return status == OrderStatus.OPEN || status == OrderStatus.MATCHING || status == OrderStatus.PARTIALLY_FILLED
				|| status == OrderStatus.CANCEL_REQUESTED;
	}

}
