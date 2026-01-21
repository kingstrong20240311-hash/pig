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

package com.pig4cloud.pig.order.match.handler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.error.annotation.ErrorHandler;
import com.pig4cloud.pig.order.api.dto.CommitMatchRequest;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.match.OrderMatchService;
import com.pig4cloud.pig.order.match.dto.FailedReduceEventDTO;
import com.pig4cloud.pig.order.match.dto.FailedRejectEventDTO;
import com.pig4cloud.pig.order.match.dto.FailedTradeEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 交易事件错误处理器
 * <p>
 * 用于补偿处理失败的交易事件
 * </p>
 *
 * @author lengleng
 * @date 2025/01/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventErrorHandler {

	private static final BigDecimal SCALE_FACTOR = BigDecimal.valueOf(100);

	private final OrderMatchService orderMatchService;

	private final OrderMapper orderMapper;

	/**
	 * 处理失败的交易事件补偿
	 * <p>
	 * 当交易事件处理失败后,可通过此方法重试提交匹配
	 * </p>
	 * @param failedEvent 失败的交易事件
	 */
	@ErrorHandler(domain = "order", key = "handleTradeEvent", payloadClass = FailedTradeEventDTO.class)
	public void compensateFailedTradeEvent(FailedTradeEventDTO failedEvent) {
		log.info("Compensating failed trade event: matchId={}, takerOrderId={}", failedEvent.getMatchId(),
				failedEvent.getTakerOrderId());

		// 重新构建 CommitMatchRequest
		CommitMatchRequest request = new CommitMatchRequest();
		request.setMatchId(failedEvent.getMatchId());
		request.setTakerOrderId(failedEvent.getTakerOrderId());
		request.setFills(failedEvent.getFills());
		request.setIdempotencyKey(failedEvent.getIdempotencyKey());

		// 重新提交匹配
		orderMatchService.commitMatch(request);

		log.info("Trade event compensation successful: matchId={}", failedEvent.getMatchId());
	}

	/**
	 * 处理失败的减量事件补偿
	 * <p>
	 * 当减量事件处理失败后,可通过此方法重试更新订单状态
	 * </p>
	 * @param failedEvent 失败的减量事件
	 */
	@ErrorHandler(domain = "order", key = "handleReduceEvent", payloadClass = FailedReduceEventDTO.class)
	public void compensateFailedReduceEvent(FailedReduceEventDTO failedEvent) {
		log.info("Compensating failed reduce event: orderId={}, reducedVolume={}, orderCompleted={}",
				failedEvent.getOrderId(), failedEvent.getReducedVolume(), failedEvent.getOrderCompleted());

		// Get the order
		Order order = orderMapper.selectById(failedEvent.getOrderId());
		if (order == null) {
			log.error("Order not found during reduce event compensation: orderId={}", failedEvent.getOrderId());
			throw new IllegalStateException("Order not found: " + failedEvent.getOrderId());
		}

		// Update order status based on whether it's completed
		if (Boolean.TRUE.equals(failedEvent.getOrderCompleted())) {
			order.setStatus(OrderStatus.CANCELLED);
			order.setRemainingQuantity(BigDecimal.ZERO);
		}
		else {
			// Partial cancel - update remaining quantity
			BigDecimal reducedVolume = convertLongToDecimal(failedEvent.getReducedVolume());
			BigDecimal newRemaining = order.getRemainingQuantity().subtract(reducedVolume);
			order.setRemainingQuantity(newRemaining);
		}

		orderMapper.updateById(order);
		log.info("Reduce event compensation successful: orderId={}, newStatus={}", failedEvent.getOrderId(),
				order.getStatus());
	}

	/**
	 * 处理失败的拒绝事件补偿
	 * <p>
	 * 当拒绝事件处理失败后,可通过此方法重试更新订单状态
	 * </p>
	 * @param failedEvent 失败的拒绝事件
	 */
	@ErrorHandler(domain = "order", key = "handleRejectEvent", payloadClass = FailedRejectEventDTO.class)
	public void compensateFailedRejectEvent(FailedRejectEventDTO failedEvent) {
		log.info("Compensating failed reject event: orderId={}, rejectedVolume={}", failedEvent.getOrderId(),
				failedEvent.getRejectedVolume());

		// Get the order
		Order order = orderMapper.selectById(failedEvent.getOrderId());
		if (order == null) {
			log.error("Order not found during reject event compensation: orderId={}", failedEvent.getOrderId());
			throw new IllegalStateException("Order not found: " + failedEvent.getOrderId());
		}

		// Mark order as rejected (for IOC orders that couldn't be filled)
		order.setStatus(OrderStatus.REJECTED);
		order.setRejectReason("IOC order could not be filled at specified price");

		orderMapper.updateById(order);
		log.info("Reject event compensation successful: orderId={}", failedEvent.getOrderId());
	}

	/**
	 * Convert exchange-core long to BigDecimal
	 */
	private static BigDecimal convertLongToDecimal(long value) {
		return BigDecimal.valueOf(value).divide(SCALE_FACTOR);
	}

}
