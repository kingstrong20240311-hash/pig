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

import exchange.core2.core.IEventsHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order Events Handler for ExchangeCore matching engine callbacks
 *
 * @author lengleng
 * @date 2025/01/18
 */
@Slf4j
@RequiredArgsConstructor
public class OrderEventsHandler implements IEventsHandler {

	private final OrderMatchService orderMatchService;

	@Override
	public void commandResult(ApiCommandResult commandResult) {
		orderMatchService.handleCommandResult(commandResult);
	}

	@Override
	public void tradeEvent(TradeEvent tradeEvent) {
		orderMatchService.handleTradeEvent(tradeEvent);
	}

	@Override
	public void rejectEvent(RejectEvent rejectEvent) {
		orderMatchService.handleRejectEvent(rejectEvent);
	}

	@Override
	public void reduceEvent(ReduceEvent reduceEvent) {
		orderMatchService.handleReduceEvent(reduceEvent);
	}

	@Override
	public void orderBook(OrderBook orderBook) {
		// Not used for now - can be implemented later for market data feeds
		log.trace("OrderBook snapshot received for symbol={}", orderBook.symbol);
	}

}
