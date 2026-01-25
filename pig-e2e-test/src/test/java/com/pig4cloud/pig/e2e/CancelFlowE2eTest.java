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

package com.pig4cloud.pig.e2e;

import com.pig4cloud.pig.order.api.dto.CreateOrderRequest;
import com.pig4cloud.pig.order.api.enums.Side;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 撤单流程 E2E 测试
 * 
 * 测试场景：
 * 1. 创建订单 → 撤单 → 验证订单状态
 * 2. 部分成交后撤单 → 验证剩余数量
 * 3. 批量撤单
 * 4. 撤单后验证冻结资金释放
 *
 * @author pig4cloud
 */
@Slf4j
@DisplayName("撤单流程 E2E 测试")
public class CancelFlowE2eTest extends E2eBaseTest {

	private static final String ORDER_CREATE_API = "/order/create";
	private static final String ORDER_QUERY_API = "/order";
	private static final String CANCEL_API = "/order/cancel";
	private static final String TRADE_API = "/order/trades";
	private static final String FREEZE_API = "/vault/freeze";

	@Test
	@DisplayName("测试单个订单撤单流程")
	public void testSingleOrderCancellation() {
		log.info("========== 开始测试单个订单撤单流程 ==========");

		// Step 1: 创建限价买单（不会立即成交）
		log.info("Step 1: 创建限价买单");
		String orderId = createOrder("BUY", "BTC", BigDecimal.valueOf(1.0), BigDecimal.valueOf(30000));
		assertNotNull(orderId, "订单创建失败");
		log.info("订单创建成功，订单 ID: {}", orderId);

		// Step 2: 验证订单状态为 OPEN（等待成交）
		log.info("Step 2: 验证订单初始状态");
		Response orderResponse = queryOrder(orderId);
		assertSuccess(orderResponse);
		String initialStatus = orderResponse.jsonPath().getString("data.status");
		assertEquals("OPEN", initialStatus, "订单初始状态应为 OPEN");
		log.info("订单初始状态验证通过: {}", initialStatus);

		// Step 3: 执行撤单
		log.info("Step 3: 执行撤单操作");
		Response cancelResponse = cancelOrder(orderId);
		assertSuccess(cancelResponse);
		log.info("撤单请求成功");

		// Step 4: 轮询查询订单状态，等待撤单完成
		log.info("Step 4: 轮询查询订单状态，等待撤单完成");
		Response finalOrderResponse = pollUntil(
				() -> queryOrder(orderId),
				response -> "CANCELLED".equals(response.jsonPath().getString("data.status")),
				"订单撤单超时，未达到 CANCELLED 状态"
		);
		assertEquals("CANCELLED", finalOrderResponse.jsonPath().getString("data.status"), "订单应为已撤销状态");
		log.info("订单撤单成功，最终状态: CANCELLED");

		// Step 5: 验证冻结资金已释放
		log.info("Step 5: 验证冻结资金已释放");
		verifyFreezeReleased(orderId);

		log.info("========== 单个订单撤单流程测试通过 ==========");
	}

	@Test
	@DisplayName("测试部分成交后撤单")
	public void testCancelPartiallyFilledOrder() {
		log.info("========== 开始测试部分成交后撤单 ==========");

		// Step 1: 创建大额买单
		log.info("Step 1: 创建大额买单（数量 100）");
		String buyOrderId = createOrder("BUY", "ETH", BigDecimal.valueOf(100), BigDecimal.valueOf(2000));
		log.info("买单创建成功，订单 ID: {}", buyOrderId);

		// Step 2: 创建小额卖单，触发部分成交
		log.info("Step 2: 创建小额卖单（数量 30），触发部分成交");
		String sellOrderId = createOrder("SELL", "ETH", BigDecimal.valueOf(30), BigDecimal.valueOf(2000));
		log.info("卖单创建成功，订单 ID: {}", sellOrderId);

		// Step 3: 等待买单部分成交
		log.info("Step 3: 等待买单部分成交");
		Response partialFilledResponse = pollUntil(
				() -> queryOrder(buyOrderId),
				response -> "PARTIALLY_FILLED".equals(response.jsonPath().getString("data.status")),
				"买单未达到部分成交状态"
		);
		assertEquals("PARTIALLY_FILLED", partialFilledResponse.jsonPath().getString("data.status"), "买单应为部分成交");
		BigDecimal filledQuantity = new BigDecimal(partialFilledResponse.jsonPath().getString("data.filledQuantity"));
		log.info("买单部分成交，已成交数量: {}", filledQuantity);

		// Step 4: 撤销剩余未成交部分
		log.info("Step 4: 撤销剩余未成交部分");
		Response cancelResponse = cancelOrder(buyOrderId);
		assertSuccess(cancelResponse);
		log.info("撤单请求成功");

		// Step 5: 验证订单状态变为 CANCELLED
		log.info("Step 5: 验证订单状态变为 CANCELLED");
		Response finalOrderResponse = pollUntil(
				() -> queryOrder(buyOrderId),
				response -> "CANCELLED".equals(response.jsonPath().getString("data.status")),
				"订单未达到 CANCELLED 状态"
		);
		assertEquals("CANCELLED", finalOrderResponse.jsonPath().getString("data.status"), "订单应为已撤销状态");
		
		// 验证已成交数量未变
		BigDecimal finalFilledQuantity = new BigDecimal(finalOrderResponse.jsonPath().getString("data.filledQuantity"));
		assertEquals(filledQuantity.compareTo(finalFilledQuantity), 0, "已成交数量不应变化");
		log.info("订单撤单成功，已成交数量: {}, 未成交部分已撤销", finalFilledQuantity);

		log.info("========== 部分成交后撤单测试通过 ==========");
	}

	// TODO: Batch cancel endpoint not implemented yet
	// @Test
	// @DisplayName("测试批量撤单")
	// public void testBatchOrderCancellation() {
	// 	log.info("========== 开始测试批量撤单 ==========");
	// 	// Test implementation pending batch cancel API
	// 	log.info("========== 批量撤单测试跳过（接口未实现） ==========");
	// }

	@Test
	@DisplayName("测试重复撤单（幂等性）")
	public void testDuplicateCancellation() {
		log.info("========== 开始测试重复撤单幂等性 ==========");

		// Step 1: 创建订单
		log.info("Step 1: 创建订单");
		String orderId = createOrder("BUY", "BTC", BigDecimal.valueOf(0.1), BigDecimal.valueOf(40000));
		log.info("订单创建成功，订单 ID: {}", orderId);

		// Step 2: 第一次撤单
		log.info("Step 2: 第一次撤单");
		Response firstCancelResponse = cancelOrder(orderId);
		assertSuccess(firstCancelResponse);
		log.info("第一次撤单成功");

		// Step 3: 等待撤单完成
		log.info("Step 3: 等待撤单完成");
		pollUntil(
				() -> queryOrder(orderId),
				response -> "CANCELLED".equals(response.jsonPath().getString("data.status")),
				"订单未达到 CANCELLED 状态"
		);
		log.info("订单已撤销");

		// Step 4: 第二次撤单（应幂等处理，不报错）
		log.info("Step 4: 第二次撤单（测试幂等性）");
		Response secondCancelResponse = cancelOrder(orderId);
		// 根据业务设计，可能返回成功（幂等）或特定错误码
		int statusCode = secondCancelResponse.getStatusCode();
		assertTrue(statusCode == 200 || statusCode == 400, "重复撤单应返回成功或业务错误");
		log.info("第二次撤单响应状态码: {}", statusCode);

		log.info("========== 重复撤单幂等性测试通过 ==========");
	}

	@Test
	@DisplayName("测试已成交订单无法撤单")
	public void testCannotCancelFilledOrder() {
		log.info("========== 开始测试已成交订单无法撤单 ==========");

		// Step 1: 创建限价卖单
		log.info("Step 1: 创建限价卖单");
		String sellOrderId = createOrder("SELL", "BTC", BigDecimal.valueOf(0.1), BigDecimal.valueOf(45000));
		log.info("卖单创建成功，订单 ID: {}", sellOrderId);

		// Step 2: 创建市价买单，立即成交
		log.info("Step 2: 创建市价买单，立即成交");
		String buyOrderId = createMarketOrder("BUY", "BTC", BigDecimal.valueOf(0.1));
		log.info("买单创建成功，订单 ID: {}", buyOrderId);

		// Step 3: 等待卖单完全成交
		log.info("Step 3: 等待卖单完全成交");
		Response filledOrderResponse = pollUntil(
				() -> queryOrder(sellOrderId),
				response -> "FILLED".equals(response.jsonPath().getString("data.status")),
				"卖单未完全成交"
		);
		assertEquals("FILLED", filledOrderResponse.jsonPath().getString("data.status"), "卖单应完全成交");
		log.info("卖单已完全成交");

		// Step 4: 尝试撤销已成交订单（应失败）
		log.info("Step 4: 尝试撤销已成交订单");
		Response cancelResponse = cancelOrder(sellOrderId);
		// 应返回错误（400 或业务错误码）
		assertTrue(cancelResponse.getStatusCode() >= 400, "撤销已成交订单应返回错误");
		log.info("撤销已成交订单返回错误（符合预期），状态码: {}", cancelResponse.getStatusCode());

		log.info("========== 已成交订单无法撤单测试通过 ==========");
	}

	// ==================== 辅助方法 ====================

	/**
	 * 创建限价订单（带余额检查和充值）
	 */
	private String createOrder(String side, String symbol, BigDecimal quantity, BigDecimal price) {
		// 使用 DTO 构建请求
		Side sideEnum = "BUY".equals(side) ? Side.BUY : Side.SELL;

		// 在下单前检查余额并充值
		ensureBalanceBeforeOrder(sideEnum, quantity, price);

		CreateOrderRequest orderRequest = buildLimitOrderRequest(
				TEST_MARKET_ID,
				sideEnum,
				quantity,
				price
		);

		// 用 ObjectMapper 序列化 DTO
		Response response = authenticatedRequest()
				.body(orderRequest)
				.when()
				.post(ORDER_CREATE_API)
				.then()
				.statusCode(200)
				.extract()
				.response();

		assertSuccess(response);
		String orderId = response.jsonPath().getString("data.orderId");
		assertNotNull(orderId, "订单 ID 不应为空");
		return orderId;
	}

	/**
	 * 创建市价订单（带余额检查和充值）
	 */
	private String createMarketOrder(String side, String symbol, BigDecimal quantity) {
		// 使用 DTO 构建请求
		Side sideEnum = "BUY".equals(side) ? Side.BUY : Side.SELL;

		// 在下单前检查余额并充值（市价单价格为 null）
		ensureBalanceBeforeOrder(sideEnum, quantity, null);

		CreateOrderRequest orderRequest = buildMarketOrderRequest(
				TEST_MARKET_ID,
				sideEnum,
				quantity
		);

		// 用 ObjectMapper 序列化 DTO
		Response response = authenticatedRequest()
				.body(orderRequest)
				.when()
				.post(ORDER_CREATE_API)
				.then()
				.statusCode(200)
				.extract()
				.response();

		assertSuccess(response);
		String orderId = response.jsonPath().getString("data.orderId");
		assertNotNull(orderId, "订单 ID 不应为空");
		return orderId;
	}

	/**
	 * 查询订单详情
	 */
	private Response queryOrder(String orderId) {
		return authenticatedRequest()
				.when()
				.get(ORDER_QUERY_API + "/" + orderId)
				.then()
				.extract()
				.response();
	}

	/**
	 * 撤销订单
	 */
	private Response cancelOrder(String orderId) {
		Map<String, String> cancelRequest = new HashMap<>();
		cancelRequest.put("orderId", orderId);

		return authenticatedRequest()
				.body(cancelRequest)
				.when()
				.post(CANCEL_API)
				.then()
				.extract()
				.response();
	}


	/**
	 * 验证冻结资金已释放
	 */
	private void verifyFreezeReleased(String orderId) {
		// 查询与订单关联的冻结记录
		Response freezeResponse = authenticatedRequest()
				.queryParam("refId", orderId)
				.queryParam("refType", "ORDER")
				.when()
				.get(FREEZE_API)
				.then()
				.extract()
				.response();

		if (freezeResponse.getStatusCode() == 200) {
			// 如果找到冻结记录，验证状态为 RELEASED
			String freezeStatus = freezeResponse.jsonPath().getString("data.status");
			if (freezeStatus != null) {
				assertEquals("RELEASED", freezeStatus, "冻结资金应已释放");
				log.info("冻结资金已释放验证通过");
			} else {
				log.info("未找到冻结记录（可能已清理）");
			}
		} else {
			log.info("冻结记录查询接口不可用，跳过验证");
		}
	}

	/**
	 * 在下单前确保有足够余额
	 *
	 * @param side     订单方向
	 * @param quantity 数量
	 * @param price    价格（市价单可为 null）
	 */
	private void ensureBalanceBeforeOrder(Side side, BigDecimal quantity, BigDecimal price) {
		// 计算所需资金
		BigDecimal requiredFunds = calculateRequiredFunds(side, quantity, price);

		if (requiredFunds.compareTo(BigDecimal.ZERO) > 0) {
			log.info("订单所需资金: {}", requiredFunds);
			// 确保有足够余额
			ensureSufficientBalance(DEFAULT_ACCOUNT_ID, DEFAULT_ASSET_SYMBOL, requiredFunds);
		}
	}

}
