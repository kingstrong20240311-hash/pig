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

import com.pig4cloud.pig.order.api.dto.CommitMatchRequest;
import com.pig4cloud.pig.order.api.dto.FillDTO;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.OrderService;
import exchange.core2.core.IEventsHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Match Service Implementation
 *
 * @author lengleng
 * @date 2025/01/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMatchServiceImpl implements OrderMatchService {

	private final OrderService orderService;

	private final OrderMapper orderMapper;

	@Override
	public void handleTradeEvent(IEventsHandler.TradeEvent tradeEvent) {
		log.info("Handling trade event: takerOrderId={}, totalVolume={}, tradesCount={}", tradeEvent.takerOrderId,
				tradeEvent.totalVolume, tradeEvent.trades.size());

		try {
			// Generate a unique matchId using timestamp and takerOrderId
			String matchId = String.format("%d-%d", tradeEvent.takerOrderId, tradeEvent.timestamp);

			// Use matchId as idempotency key
			String idempotencyKey = matchId;

			// Convert trades to FillDTOs
			List<FillDTO> fills = new ArrayList<>();
			for (IEventsHandler.Trade trade : tradeEvent.trades) {
				FillDTO fillDTO = new FillDTO();
				fillDTO.setMakerOrderId(trade.makerOrderId);
				// Convert from exchange-core long format to BigDecimal
				// Assuming price and volume are in the smallest units (e.g., cents)
				fillDTO.setPrice(BigDecimal.valueOf(trade.price));
				fillDTO.setQuantity(BigDecimal.valueOf(trade.volume));
				fillDTO.setFee(BigDecimal.ZERO); // TODO: Calculate fees if needed
				fills.add(fillDTO);
			}

			// Create commit match request
			CommitMatchRequest request = new CommitMatchRequest();
			request.setMatchId(matchId);
			request.setTakerOrderId(tradeEvent.takerOrderId);
			request.setFills(fills);
			request.setIdempotencyKey(idempotencyKey);

			// Call existing commitMatch method
			orderService.commitMatch(request);

			log.info("Trade event processed successfully: matchId={}, takerOrderId={}", matchId,
					tradeEvent.takerOrderId);
		}
		catch (Exception e) {
			log.error("Failed to handle trade event: takerOrderId={}", tradeEvent.takerOrderId, e);
			// TODO: Implement retry mechanism or dead letter queue for failed trade
			// events
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
				// TODO: Handle missing order - may need to investigate data inconsistency
				return;
			}

			// Update order status based on whether it's completed
			if (reduceEvent.orderCompleted) {
				order.setStatus(OrderStatus.CANCELLED);
				order.setRemainingQuantity(BigDecimal.ZERO);
			}
			else {
				// Partial cancel - update remaining quantity
				BigDecimal reducedVolume = BigDecimal.valueOf(reduceEvent.reducedVolume);
				BigDecimal newRemaining = order.getRemainingQuantity().subtract(reducedVolume);
				order.setRemainingQuantity(newRemaining);

				// If there was some fill before this cancel, keep PARTIALLY_FILLED status
				if (order.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
					order.setStatus(OrderStatus.PARTIALLY_FILLED);
				}
			}

			orderMapper.updateById(order);
			log.info("Reduce event processed successfully: orderId={}, newStatus={}", reduceEvent.orderId,
					order.getStatus());
		}
		catch (Exception e) {
			log.error("Failed to handle reduce event: orderId={}", reduceEvent.orderId, e);
			// TODO: Implement retry mechanism for failed reduce events
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
				// TODO: Handle missing order - may need to investigate data inconsistency
				return;
			}

			// Mark order as rejected (for IOC orders that couldn't be filled)
			order.setStatus(OrderStatus.REJECTED);
			order.setRejectReason("IOC order could not be filled at specified price");

			orderMapper.updateById(order);
			log.info("Reject event processed successfully: orderId={}", rejectEvent.orderId);
		}
		catch (Exception e) {
			log.error("Failed to handle reject event: orderId={}", rejectEvent.orderId, e);
			// TODO: Implement retry mechanism for failed reject events
		}
	}

	@Override
	public void handleCommandResult(IEventsHandler.ApiCommandResult result) {
		log.debug("Command result: command={}, resultCode={}, seq={}", result.command, result.resultCode, result.seq);

		// Can be used for monitoring/metrics
		// For now, we'll just log it at debug level
	}

}
