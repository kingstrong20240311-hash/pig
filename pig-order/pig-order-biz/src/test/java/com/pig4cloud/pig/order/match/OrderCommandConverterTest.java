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

}
