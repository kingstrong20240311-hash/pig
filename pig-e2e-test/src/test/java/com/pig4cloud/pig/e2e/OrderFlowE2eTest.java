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
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.api.enums.Side;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单流程 E2E 测试
 *
 * 测试场景： 1. 下单 → 订单创建成功 2. 撮合 → 订单匹配 3. 成交 → 生成交易记录 4. 验证 → 订单状态、成交记录、余额变化
 *
 * @author pig4cloud
 */
@Slf4j
@DisplayName("订单流程 E2E 测试")
public class OrderFlowE2eTest extends E2eBaseTest {

	private static final String ORDER_CREATE_API = "/order/create";

	private static final String ORDER_QUERY_API = "/order";

	private static final String TRADE_API = "/order/trades";

	private static final String BALANCE_API = "/vault/balance";

	@Test
	@DisplayName("测试完整下单流程：创建订单 → 撮合 → 成交验证")
	public void testCompleteOrderFlow() {
		log.info("========== 开始测试完整下单流程 ==========");

		// Step 1: 创建 YES 买单
		log.info("Step 1: 创建 YES 买单");
		String buyOrderId = createOrderWithOutcome(Outcome.YES, "BUY", BigDecimal.valueOf(100),
				BigDecimal.valueOf(0.6));
		assertNotNull(buyOrderId, "买单创建失败");
		log.info("YES 买单创建成功，订单 ID: {}", buyOrderId);

		// Step 2: 创建 YES 卖单（价格匹配，触发撮合）
		log.info("Step 2: 创建 YES 卖单（触发撮合）");
		String sellOrderId = createOrderWithOutcome(Outcome.YES, "SELL", BigDecimal.valueOf(50),
				BigDecimal.valueOf(0.6));
		assertNotNull(sellOrderId, "卖单创建失败");
		log.info("YES 卖单创建成功，订单 ID: {}", sellOrderId);

		// Step 3: 等待撮合完成，轮询查询买单状态
		log.info("Step 3: 等待撮合完成，轮询查询买单状态");
		Response buyOrderResponse = pollUntil(() -> queryOrder(buyOrderId),
				response -> "PARTIALLY_FILLED".equals(response.jsonPath().getString("data.status"))
						|| "FILLED".equals(response.jsonPath().getString("data.status")),
				"买单撮合超时，未达到 PARTIALLY_FILLED 或 FILLED 状态");
		log.info("买单撮合完成，当前状态: {}", buyOrderResponse.jsonPath().getString("data.status"));

		// Step 4: 查询卖单状态
		log.info("Step 4: 查询卖单状态");
		Response sellOrderResponse = queryOrder(sellOrderId);
		String sellOrderStatus = sellOrderResponse.jsonPath().getString("data.status");
		log.info("卖单当前状态: {}", sellOrderStatus);
		assertTrue("FILLED".equals(sellOrderStatus) || "PARTIALLY_FILLED".equals(sellOrderStatus),
				"卖单应为 FILLED 或 PARTIALLY_FILLED 状态");

		// Step 5: 查询成交记录
		log.info("Step 5: 查询成交记录");
		Response tradesResponse = queryTradesByOrder(buyOrderId);
		assertSuccess(tradesResponse);
		assertFieldExists(tradesResponse, "data");

		int tradeCount = tradesResponse.jsonPath().getList("data").size();
		assertTrue(tradeCount > 0, "应有至少一条成交记录");
		log.info("查询到 {} 条成交记录", tradeCount);

		// Step 6: 验证成交详情
		log.info("Step 6: 验证成交详情");
		Map<String, Object> firstTrade = tradesResponse.jsonPath().getMap("data[0]");
		assertNotNull(firstTrade.get("tradeId"), "成交 ID 不应为空");
		// 使用容差比较double，避免浮点数精度问题
		assertEquals(BigDecimal.valueOf(0.6).doubleValue(), ((Number) firstTrade.get("price")).doubleValue(), 0.0001,
				"成交价格应为 0.6");
		log.info("成交详情验证通过：成交 ID={}, 成交价格={}, 成交数量={}", firstTrade.get("tradeId"), firstTrade.get("price"),
				firstTrade.get("quantity"));

		// TODO: 用户余额检测，每个用例都应该有，但现在测试数据只有一个admin用户。后续添加多个测试用户。
		// Step 7: 查询余额变化（可选，需要管理接口支持）
//		log.info("Step 7: 查询余额变化");
//		verifyBalanceChange();

		log.info("========== 完整下单流程测试通过 ==========");
	}

	@Test
	@DisplayName("测试市价单立即成交")
	public void testMarketOrderImmediateExecution() {
		log.info("========== 开始测试市价单立即成交 ==========");

		// Step 1: 先创建限价 YES 卖单（挂单）
		log.info("Step 1: 创建限价 YES 卖单（挂单）");
		String limitSellOrderId = createOrderWithOutcome(Outcome.YES, "SELL", BigDecimal.valueOf(100),
				BigDecimal.valueOf(0.7));
		log.info("限价 YES 卖单创建成功，订单 ID: {}", limitSellOrderId);

		// Step 2: 确认挂单进入 MATCHING 状态
		log.info("Step 2: 确认限价 YES 卖单进入 MATCHING 状态");
		pollUntil(() -> queryOrder(limitSellOrderId),
				response -> "MATCHING".equals(response.jsonPath().getString("data.status")),
				"限价 YES 卖单未进入 MATCHING 状态");
		log.info("限价 YES 卖单已进入 MATCHING 状态");

		// Step 3: 创建市价 YES 买单（应立即成交）
		log.info("Step 3: 创建市价 YES 买单（应立即成交）");
		String marketBuyOrderId = createMarketOrderWithOutcome(Outcome.YES, "BUY", BigDecimal.valueOf(100));
		log.info("市价 YES 买单创建成功，订单 ID: {}", marketBuyOrderId);

		// Step 4: 验证市价单立即完全成交
		log.info("Step 4: 验证市价单立即完全成交");
		Response marketOrderResponse = pollUntil(() -> queryOrder(marketBuyOrderId),
				response -> "FILLED".equals(response.jsonPath().getString("data.status")), "市价 YES 买单未完全成交");
		assertEquals("FILLED", marketOrderResponse.jsonPath().getString("data.status"), "市价单应立即完全成交");
		log.info("市价单已完全成交");

		log.info("========== 市价单立即成交测试通过 ==========");
	}

	@Test
	@DisplayName("测试订单部分成交")
	public void testPartialOrderFill() {
		log.info("========== 开始测试订单部分成交 ==========");

		// Step 1: 创建大额买单
		log.info("Step 1: 创建大额买单（数量 100）");
		String buyOrderId = createOrderWithOutcome(Outcome.YES, "BUY", BigDecimal.valueOf(100),
				BigDecimal.valueOf(0.6));
		log.info("大额买单创建成功，订单 ID: {}", buyOrderId);

		// Step 2: 创建小额卖单（只能部分成交）
		log.info("Step 2: 创建小额卖单（数量 30，只能部分成交买单）");
		String sellOrderId = createOrderWithOutcome(Outcome.YES, "SELL", BigDecimal.valueOf(30),
				BigDecimal.valueOf(0.6));
		log.info("小额卖单创建成功，订单 ID: {}", sellOrderId);

		// Step 3: 验证买单部分成交
		log.info("Step 3: 验证买单部分成交");
		Response buyOrderResponse = pollUntil(() -> queryOrder(buyOrderId),
				response -> "PARTIALLY_FILLED".equals(response.jsonPath().getString("data.status")), "买单应为部分成交状态");
		assertEquals("PARTIALLY_FILLED", buyOrderResponse.jsonPath().getString("data.status"), "买单应为部分成交");

		// 验证已成交数量

		BigDecimal remainingQuantity = new BigDecimal(buyOrderResponse.jsonPath().getString("data.remainingQuantity"));
		BigDecimal filledQuantity = BigDecimal.valueOf(100).subtract(remainingQuantity);
		assertEquals(0, BigDecimal.valueOf(30).compareTo(filledQuantity), "已成交数量应为 30");
		log.info("买单部分成交验证通过，已成交数量: {}", filledQuantity);

		// Step 4: 验证卖单完全成交
		log.info("Step 4: 验证卖单完全成交");
		Response sellOrderResponse = queryOrder(sellOrderId);
		assertEquals("FILLED", sellOrderResponse.jsonPath().getString("data.status"), "卖单应完全成交");
		log.info("卖单已完全成交");

		log.info("========== 订单部分成交测试通过 ==========");
	}

	@Test
	@DisplayName("测试 YES/NO 订单簿独立性")
	public void testYesNoOrderBookIndependence() {
		log.info("========== 开始测试 YES/NO 订单簿独立性 ==========");

		// Step 1: 创建 YES 买单
		log.info("Step 1: 创建 YES 买单（价格 0.6）");
		String yesBuyOrderId = createOrderWithOutcome(Outcome.YES, "BUY", BigDecimal.valueOf(100),
				BigDecimal.valueOf(0.6));
		log.info("YES 买单创建成功，订单 ID: {}", yesBuyOrderId);

		// Step 2: 创建 NO 买单（相同价格）
		log.info("Step 2: 创建 NO 买单（价格 0.6）");
		String noBuyOrderId = createOrderWithOutcome(Outcome.NO, "BUY", BigDecimal.valueOf(100),
				BigDecimal.valueOf(0.6));
		log.info("NO 买单创建成功，订单 ID: {}", noBuyOrderId);

		// Step 3: 创建 YES 卖单（应该只与 YES 买单匹配）
		log.info("Step 3: 创建 YES 卖单（价格 0.6，应该只与 YES 买单匹配）");
		String yesSellOrderId = createOrderWithOutcome(Outcome.YES, "SELL", BigDecimal.valueOf(50),
				BigDecimal.valueOf(0.6));
		log.info("YES 卖单创建成功，订单 ID: {}", yesSellOrderId);

		// Step 4: 验证 YES 买单部分成交
		log.info("Step 4: 验证 YES 买单部分成交");
		Response yesBuyResponse = pollUntil(() -> queryOrder(yesBuyOrderId),
				response -> "PARTIALLY_FILLED".equals(response.jsonPath().getString("data.status"))
						|| "FILLED".equals(response.jsonPath().getString("data.status")),
				"YES 买单应被成交");
		log.info("YES 买单状态: {}", yesBuyResponse.jsonPath().getString("data.status"));

		// Step 5: 验证 NO 买单仍然未成交（因为 YES 卖单不会与 NO 买单匹配）
		log.info("Step 5: 验证 NO 买单仍然未成交");
		Response noBuyResponse = queryOrder(noBuyOrderId);
		String noBuyStatus = noBuyResponse.jsonPath().getString("data.status");
		assertTrue("MATCHING".equals(noBuyStatus) || "NEW".equals(noBuyStatus),
				"NO 买单不应与 YES 卖单匹配，应保持 MATCHING 或 NEW 状态，实际状态: " + noBuyStatus);
		log.info("NO 买单状态验证通过: {}", noBuyStatus);

		// Step 6: 创建 NO 卖单（应该只与 NO 买单匹配）
		log.info("Step 6: 创建 NO 卖单（价格 0.6，应该只与 NO 买单匹配）");
		String noSellOrderId = createOrderWithOutcome(Outcome.NO, "SELL", BigDecimal.valueOf(50),
				BigDecimal.valueOf(0.6));
		log.info("NO 卖单创建成功，订单 ID: {}", noSellOrderId);

		// Step 7: 验证 NO 买单部分成交
		log.info("Step 7: 验证 NO 买单部分成交");
		Response noBuyFinalResponse = pollUntil(() -> queryOrder(noBuyOrderId),
				response -> "PARTIALLY_FILLED".equals(response.jsonPath().getString("data.status"))
						|| "FILLED".equals(response.jsonPath().getString("data.status")),
				"NO 买单应被成交");
		log.info("NO 买单最终状态: {}", noBuyFinalResponse.jsonPath().getString("data.status"));

		log.info("========== YES/NO 订单簿独立性测试通过 ==========");
	}

	// ==================== 辅助方法 ====================

	/**
	 * 创建限价订单（支持指定 Outcome）
	 */
	private String createOrderWithOutcome(Outcome outcome, String side, BigDecimal quantity, BigDecimal price) {
		// 使用 DTO 构建请求
		Side sideEnum = "BUY".equals(side) ? Side.BUY : Side.SELL;

		// 在下单前检查余额并充值
		if (sideEnum == Side.BUY) {
			// BUY 订单需要 USDC
			ensureBalanceBeforeOrder(sideEnum, quantity, price);
		}
		else {
			// SELL 订单需要对应的 outcome 资产（YES 或 NO）
			ensureSufficientOutcomeAsset(DEFAULT_ACCOUNT_ID, TEST_MARKET_ID, outcome, quantity);
		}

		CreateOrderRequest orderRequest = buildLimitOrderRequest(TEST_MARKET_ID, outcome, sideEnum, quantity, price);

		// 用 ObjectMapper 序列化 DTO
		Response response = authenticatedRequest().body(orderRequest)
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
	 * 创建市价订单（支持指定 Outcome）
	 */
	private String createMarketOrderWithOutcome(Outcome outcome, String side, BigDecimal quantity) {
		// 使用 DTO 构建请求
		Side sideEnum = "BUY".equals(side) ? Side.BUY : Side.SELL;

		// 在下单前检查余额并充值
		if (sideEnum == Side.BUY) {
			// BUY 订单需要 USDC（市价单价格为 null）
			ensureBalanceBeforeOrder(sideEnum, quantity, null);
		}
		else {
			// SELL 订单需要对应的 outcome 资产（YES 或 NO）
			ensureSufficientOutcomeAsset(DEFAULT_ACCOUNT_ID, TEST_MARKET_ID, outcome, quantity);
		}

		CreateOrderRequest orderRequest = buildMarketOrderRequest(TEST_MARKET_ID, outcome, sideEnum, quantity);

		// 用 ObjectMapper 序列化 DTO
		Response response = authenticatedRequest().body(orderRequest)
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
		return authenticatedRequest().when()
			.get(ORDER_QUERY_API + "/" + orderId)
			.then()
			.statusCode(200)
			.extract()
			.response();
	}

	/**
	 * 查询订单的成交记录
	 */
	private Response queryTradesByOrder(String orderId) {
		return authenticatedRequest().queryParam("orderId", orderId).when().get(TRADE_API).then().extract().response();
	}

	/**
	 * 验证余额变化（需要管理接口支持）
	 */
	private void verifyBalanceChange() {
		// 查询当前用户余额
		Response balanceResponse = authenticatedRequest().when()
			.get(BALANCE_API)
			.then()
			.statusCode(200)
			.extract()
			.response();

		assertSuccess(balanceResponse);
		log.info("余额查询成功，当前余额: {}", balanceResponse.getBody().asString());
	}

	/**
	 * 在下单前确保有足够余额
	 * @param side 订单方向
	 * @param quantity 数量
	 * @param price 价格（市价单可为 null）
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
