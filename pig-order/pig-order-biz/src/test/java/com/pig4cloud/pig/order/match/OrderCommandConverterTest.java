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
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import exchange.core2.core.common.api.ApiPlaceOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderCommandConverter
 *
 * @author lengleng
 * @date 2025/01/19
 */
@DisplayName("OrderCommandConverter Unit Tests")
class OrderCommandConverterTest {

	/**
	 * U-01: Test price and quantity scaling
	 */
	@Test
	@DisplayName("U-01: Price and quantity scaling rules")
	void testPriceAndQuantityScaling() {
		// Given
		Order order = new Order();
		order.setOrderId(1000L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setSide(Side.BUY);
		order.setOrderType(OrderType.LIMIT);
		order.setPrice(new BigDecimal("100.12"));
		order.setQuantity(new BigDecimal("5.34"));
		order.setTimeInForce(TimeInForce.GTC);

		int symbolId = 1;

		// When
		ApiPlaceOrder apiOrder = OrderCommandConverter.toApiPlaceOrder(order, symbolId);

		// Then
		assertThat(apiOrder.price).isEqualTo(10012L); // 100.12 * 100
		assertThat(apiOrder.size).isEqualTo(534L); // 5.34 * 100
	}

	/**
	 * U-02: Test BUY reserve price calculation (+20%)
	 */
	@Test
	@DisplayName("U-02: BUY reserve price +20%")
	void testBuyReservePrice() {
		// Given
		Order order = new Order();
		order.setOrderId(1001L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setSide(Side.BUY);
		order.setOrderType(OrderType.LIMIT);
		order.setPrice(new BigDecimal("100.00"));
		order.setQuantity(new BigDecimal("10.00"));
		order.setTimeInForce(TimeInForce.GTC);

		int symbolId = 1;

		// When
		ApiPlaceOrder apiOrder = OrderCommandConverter.toApiPlaceOrder(order, symbolId);

		// Then
		long expectedPrice = 10000L; // 100.00 * 100
		long expectedReservePrice = 12000L; // 10000 * 1.2
		assertThat(apiOrder.price).isEqualTo(expectedPrice);
		assertThat(apiOrder.reservePrice).isEqualTo(expectedReservePrice);
	}

	/**
	 * U-03: Test SELL reserve price calculation (-20%)
	 */
	@Test
	@DisplayName("U-03: SELL reserve price -20%")
	void testSellReservePrice() {
		// Given
		Order order = new Order();
		order.setOrderId(1002L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setSide(Side.SELL);
		order.setOrderType(OrderType.LIMIT);
		order.setPrice(new BigDecimal("100.00"));
		order.setQuantity(new BigDecimal("10.00"));
		order.setTimeInForce(TimeInForce.GTC);

		int symbolId = 1;

		// When
		ApiPlaceOrder apiOrder = OrderCommandConverter.toApiPlaceOrder(order, symbolId);

		// Then
		long expectedPrice = 10000L; // 100.00 * 100
		long expectedReservePrice = 8000L; // 10000 * 0.8
		assertThat(apiOrder.price).isEqualTo(expectedPrice);
		assertThat(apiOrder.reservePrice).isEqualTo(expectedReservePrice);
	}

	/**
	 * U-04: Test MARKET BUY order uses maximum price
	 */
	@Test
	@DisplayName("U-04: MARKET BUY order uses maximum price")
	void testMarketBuyOrder() {
		// Given: Market BUY order with null price
		Order order = new Order();
		order.setOrderId(1003L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setSide(Side.BUY);
		order.setOrderType(OrderType.MARKET);
		order.setPrice(null); // Market order has no price
		order.setQuantity(new BigDecimal("10.00"));
		order.setTimeInForce(TimeInForce.IOC);

		int symbolId = 1;

		// When
		ApiPlaceOrder apiOrder = OrderCommandConverter.toApiPlaceOrder(order, symbolId);

		// Then: Should use maximum price (10000 = 100.00 * 100)
		long expectedPrice = 10000L;
		long expectedReservePrice = 12000L; // 10000 * 1.2
		assertThat(apiOrder.price).isEqualTo(expectedPrice);
		assertThat(apiOrder.reservePrice).isEqualTo(expectedReservePrice);
		assertThat(apiOrder.orderType).isEqualTo(exchange.core2.core.common.OrderType.IOC);
	}

	/**
	 * U-05: Test MARKET SELL order uses minimum price
	 */
	@Test
	@DisplayName("U-05: MARKET SELL order uses minimum price")
	void testMarketSellOrder() {
		// Given: Market SELL order with null price
		Order order = new Order();
		order.setOrderId(1004L);
		order.setUserId(100L);
		order.setMarketId(1L);
		order.setSide(Side.SELL);
		order.setOrderType(OrderType.MARKET);
		order.setPrice(null); // Market order has no price
		order.setQuantity(new BigDecimal("10.00"));
		order.setTimeInForce(TimeInForce.IOC);

		int symbolId = 1;

		// When
		ApiPlaceOrder apiOrder = OrderCommandConverter.toApiPlaceOrder(order, symbolId);

		// Then: Should use minimum price (1 = 0.01 * 100)
		long expectedPrice = 1L;
		long expectedReservePrice = 0L; // 1 * 0.8 = 0.8 -> 0 (truncated to long)
		assertThat(apiOrder.price).isEqualTo(expectedPrice);
		assertThat(apiOrder.reservePrice).isEqualTo(expectedReservePrice);
		assertThat(apiOrder.orderType).isEqualTo(exchange.core2.core.common.OrderType.IOC);
	}

}
