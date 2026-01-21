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
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import exchange.core2.core.common.OrderAction;
import exchange.core2.core.common.OrderType;
import exchange.core2.core.common.api.ApiCancelOrder;
import exchange.core2.core.common.api.ApiPlaceOrder;

/**
 * Converter between domain Order and ExchangeCore API commands
 *
 * @author lengleng
 * @date 2025/01/18
 */
public class OrderCommandConverter {

	private static final long SYSTEM_UID = 0L;

	/**
	 * Convert domain Order to ApiPlaceOrder command
	 * @param order domain order
	 * @param symbolId symbol ID for exchange core
	 * @return ApiPlaceOrder command
	 */
	public static ApiPlaceOrder toApiPlaceOrder(Order order, int symbolId) {
		long price = convertPriceToLong(order.getPrice());
		OrderAction action = convertSide(order.getSide());

		// Calculate reserve price: +20% for BUY, -20% for SELL
		long reservePrice = calculateReservePrice(price, action);

		return ApiPlaceOrder.builder()
			.orderId(order.getOrderId())
			.uid(SYSTEM_UID)
			.symbol(symbolId)
			.price(price)
			.size(convertSizeToLong(order.getQuantity()))
			.action(action)
			.orderType(convertOrderType(order.getOrderType(), order.getTimeInForce()))
			.reservePrice(reservePrice)
			.userCookie(0) // Not used for now
			.build();
	}

	/**
	 * Convert domain order ID to ApiCancelOrder command
	 * @param orderId order ID
	 * @param userId user ID
	 * @param symbolId symbol ID for exchange core
	 * @return ApiCancelOrder command
	 */
	public static ApiCancelOrder toApiCancelOrder(Long orderId, Long userId, int symbolId) {
		return ApiCancelOrder.builder().orderId(orderId).uid(SYSTEM_UID).symbol(symbolId).build();
	}

	/**
	 * Calculate reserve price based on order action BUY orders: price * 1.2 (allow up to
	 * 20% higher) SELL orders: price * 0.8 (allow up to 20% lower)
	 */
	private static long calculateReservePrice(long price, OrderAction action) {
		if (action == OrderAction.BID) {
			// BUY: reserve 20% higher
			return (long) (price * 1.2);
		}
		else {
			// SELL: reserve 20% lower
			return (long) (price * 0.8);
		}
	}

	/**
	 * Convert Side to OrderAction
	 */
	private static OrderAction convertSide(Side side) {
		return switch (side) {
			case BUY -> OrderAction.BID;
			case SELL -> OrderAction.ASK;
		};
	}

	/**
	 * Convert OrderType and TimeInForce to ExchangeCore OrderType
	 */
	private static OrderType convertOrderType(com.pig4cloud.pig.order.api.enums.OrderType orderType,
			TimeInForce timeInForce) {
		// ExchangeCore only supports GTC and IOC
		// Map our types accordingly
		if (orderType == com.pig4cloud.pig.order.api.enums.OrderType.MARKET) {
			return OrderType.IOC; // Market orders are IOC
		}

		// For LIMIT orders, check TimeInForce
		if (timeInForce == TimeInForce.IOC || timeInForce == TimeInForce.FOK) {
			return OrderType.IOC;
		}

		return OrderType.GTC; // Default to GTC for LIMIT orders
	}

	/**
	 * Convert BigDecimal price to long (scaled by 100 for 2 decimal places) TODO: Make
	 * scaling factor configurable per market
	 */
	private static long convertPriceToLong(java.math.BigDecimal price) {
		if (price == null) {
			return 0L;
		}
		// Scale by 100 to preserve 2 decimal places
		// For example: 10.50 -> 1050
		return price.multiply(java.math.BigDecimal.valueOf(100)).longValue();
	}

	/**
	 * Convert BigDecimal size to long (scaled by 100 for 2 decimal places) TODO: Make
	 * scaling factor configurable per market
	 */
	private static long convertSizeToLong(java.math.BigDecimal size) {
		if (size == null) {
			return 0L;
		}
		// Scale by 100 to preserve 2 decimal places
		return size.multiply(java.math.BigDecimal.valueOf(100)).longValue();
	}

}
