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

/**
 * Order Match Service for processing matching engine callbacks
 *
 * @author lengleng
 * @date 2025/01/18
 */
public interface OrderMatchService {

	/**
	 * Handle trade event from matching engine
	 * @param tradeEvent trade event
	 */
	void handleTradeEvent(IEventsHandler.TradeEvent tradeEvent);

	/**
	 * Handle reduce event from matching engine (for cancel/reduce operations)
	 * @param reduceEvent reduce event
	 */
	void handleReduceEvent(IEventsHandler.ReduceEvent reduceEvent);

	/**
	 * Handle reject event from matching engine (for IOC orders)
	 * @param rejectEvent reject event
	 */
	void handleRejectEvent(IEventsHandler.RejectEvent rejectEvent);

	/**
	 * Handle command result (success/failure)
	 * @param result command result
	 */
	void handleCommandResult(IEventsHandler.ApiCommandResult result);

}
