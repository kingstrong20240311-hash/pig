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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pig4cloud.pig.e2e.util.PasswordEncryptionUtil;
import com.pig4cloud.pig.order.api.dto.CreateMarketRequest;
import com.pig4cloud.pig.order.api.dto.CreateOrderRequest;
import com.pig4cloud.pig.order.api.enums.MarketStatus;
import com.pig4cloud.pig.order.api.enums.OrderType;
import com.pig4cloud.pig.order.api.enums.Outcome;
import com.pig4cloud.pig.order.api.enums.Side;
import com.pig4cloud.pig.order.api.enums.TimeInForce;
import com.pig4cloud.pig.vault.api.dto.DepositRequest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E 测试基类 提供： - 环境变量读取 - HTTP 客户端配置 - 通用鉴权方法 - 公共断言方法 - 轮询查询工具
 *
 * @author pig4cloud
 */
@Slf4j
public abstract class E2eBaseTest {

	// ==================== 环境变量配置 ====================

	/** Gateway 地址（必填） */
	protected static String gatewayUrl;

	/** OAuth2 客户端 ID */
	protected static String clientId;

	/** OAuth2 客户端密钥 */
	protected static String clientSecret;

	/** 测试用户名（可选，有默认值） */
	protected static String testUsername;

	/** 测试密码（可选，有默认值） */
	protected static String testPassword;

	/** 管理员 Token（可选，用于跳过登录） */
	protected static String adminToken;

	/** 密码加密密钥 */
	protected static String encodeKey;

	/** 连接超时时间（毫秒） */
	protected static int connectionTimeout = 30000;

	/** 读取超时时间（毫秒） */
	protected static int readTimeout = 60000;

	/** 轮询最大等待时间（秒） */
	protected static int maxWaitSeconds = 30;

	/** 轮询间隔（毫秒） */
	protected static int pollIntervalMillis = 500;

	// ==================== HTTP 客户端 ====================

	protected static RequestSpecification requestSpec;

	protected static ObjectMapper objectMapper;

	/** 当前测试的访问 Token */
	protected String accessToken;

	// ==================== 测试数据常量 ====================

	/** 测试市场 ID（会在测试开始前动态获取或创建） */
	protected static Long TEST_MARKET_ID = 1L;

	/** 默认账户 ID（用于测试） */
	protected static Long DEFAULT_ACCOUNT_ID = 1L;

	/** 默认资产符号 */
	protected static final String DEFAULT_ASSET_SYMBOL = "USDC";

	// ==================== 初始化 ====================

	@BeforeAll
	public static void setupE2eEnvironment() {
		log.info("========== 初始化 E2E 测试环境 ==========");

		// 1. 读取环境变量
		loadEnvironmentVariables();

		// 2. 校验必填配置
		validateRequiredConfig();

		// 3. 初始化 ObjectMapper
		initializeObjectMapper();

		// 4. 配置 REST Assured
		configureRestAssured();

		log.info("========== E2E 测试环境初始化完成 ==========");
	}

	/**
	 * 读取环境变量
	 */
	private static void loadEnvironmentVariables() {
		// 必填项
		gatewayUrl = getEnv("PIG_GATEWAY_URL", null);

		// OAuth2 认证配置
		clientId = getEnv("PIG_CLIENT_ID", "test");
		clientSecret = getEnv("PIG_CLIENT_SECRET", "test");
		testUsername = getEnv("PIG_TEST_USERNAME", "admin");
		testPassword = getEnv("PIG_TEST_PASSWORD", "123456");
		adminToken = getEnv("PIG_ADMIN_TOKEN", null);

		// 密码加密配置
		encodeKey = getEnv("PIG_ENCODE_KEY", "thanks,pig4cloud");

		// 超时配置
		connectionTimeout = getEnvInt("PIG_CONNECTION_TIMEOUT", 30000);
		readTimeout = getEnvInt("PIG_READ_TIMEOUT", 60000);

		// 轮询配置
		maxWaitSeconds = getEnvInt("PIG_MAX_WAIT_SECONDS", 30);
		pollIntervalMillis = getEnvInt("PIG_POLL_INTERVAL_MILLIS", 500);

		log.info("环境变量读取完成：");
		log.info("  - Gateway URL: {}", gatewayUrl);
		log.info("  - OAuth2 Client ID: {}", clientId);
		log.info("  - Test Username: {}", testUsername);
		log.info("  - Admin Token: {}", adminToken != null ? "已配置" : "未配置");
		log.info("  - Encode Key: {}", encodeKey);
		log.info("  - Connection Timeout: {}ms", connectionTimeout);
		log.info("  - Read Timeout: {}ms", readTimeout);
	}

	/**
	 * 校验必填配置
	 */
	private static void validateRequiredConfig() {
		if (StringUtils.isBlank(gatewayUrl)) {
			throw new IllegalStateException("环境变量 PIG_GATEWAY_URL 未配置！请设置 Gateway 地址");
		}
	}

	/**
	 * 初始化 JSON 序列化器
	 */
	private static void initializeObjectMapper() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	/**
	 * 配置 REST Assured
	 */
	private static void configureRestAssured() {
		RestAssured.baseURI = gatewayUrl;

		// 配置 ObjectMapper
		RestAssuredConfig config = RestAssured.config()
			.objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> objectMapper));

		// 创建默认请求规范
		requestSpec = new RequestSpecBuilder().setConfig(config)
			.setContentType(ContentType.JSON)
			.setAccept(ContentType.JSON)
			.build();

		log.info("REST Assured 配置完成，Base URI: {}", RestAssured.baseURI);
	}

	/**
	 * 每个测试开始前执行
	 */
	@BeforeEach
	public void setupTest() {
		log.info("---------- 开始测试：{} ----------", getTestName());

		// 如果配置了 adminToken，直接使用
		if (StringUtils.isNotBlank(adminToken)) {
			accessToken = adminToken;
			log.info("使用预配置的 Admin Token");
		}
		else {
			// 否则执行登录获取 Token
			accessToken = login(testUsername, testPassword);
			log.info("登录成功，获取 Access Token: {}", maskToken(accessToken));
		}

		// 为每个测试用例创建一个新的市场
		createNewMarketForTest();
	}

	// ==================== 鉴权方法 ====================

	/**
	 * 用户登录，获取访问令牌
	 * @param username 用户名
	 * @param password 密码
	 * @return 访问令牌
	 */
	protected String login(String username, String password) {
		log.info("尝试登录：username={}, clientId={}", username, clientId);

		// 加密密码
		String encryptedPassword = PasswordEncryptionUtil.encrypt(password, encodeKey);
		log.debug("密码已加密，加密后长度: {}", encryptedPassword.length());

		Response response = given().spec(requestSpec)
			.contentType(ContentType.URLENC)
			.auth()
			.preemptive()
			.basic(clientId, clientSecret) // OAuth2 客户端凭证
			.formParam("grant_type", "password")
			.formParam("username", username)
			.formParam("password", encryptedPassword)
			.when()
			.post("/auth/oauth2/token")
			.then()
			.extract()
			.response();

		// 打印响应信息用于调试
		log.info("登录响应状态码: {}", response.getStatusCode());
		if (response.getStatusCode() != 200) {
			log.error("登录失败，响应体: {}", response.getBody().asString());
		}

		// 检查状态码
		assertEquals(200, response.getStatusCode(),
				String.format("登录失败，状态码: %d, 响应: %s", response.getStatusCode(), response.getBody().asString()));

		String token = response.jsonPath().getString("access_token");
		assertNotNull(token, "登录失败：未获取到 access_token");

		return token;
	}

	/**
	 * 用户注册
	 * @param username 用户名
	 * @param password 密码
	 * @return 注册响应
	 */
	protected Response register(String username, String password) {
		log.info("尝试注册用户：username={}", username);

		Map<String, String> registerRequest = new HashMap<>();
		registerRequest.put("username", username);
		registerRequest.put("password", password);

		Response response = given().spec(requestSpec)
			.body(registerRequest)
			.when()
			.post("/auth/register")
			.then()
			.extract()
			.response();

		log.info("注册请求完成，状态码：{}", response.getStatusCode());
		return response;
	}

	/**
	 * 用户注册（带额外信息）
	 * @param username 用户名
	 * @param password 密码
	 * @param email 邮箱
	 * @param phone 手机号
	 * @return 注册响应
	 */
	protected Response register(String username, String password, String email, String phone) {
		log.info("尝试注册用户：username={}, email={}, phone={}", username, email, phone);

		Map<String, String> registerRequest = new HashMap<>();
		registerRequest.put("username", username);
		registerRequest.put("password", password);
		if (StringUtils.isNotBlank(email)) {
			registerRequest.put("email", email);
		}
		if (StringUtils.isNotBlank(phone)) {
			registerRequest.put("phone", phone);
		}

		Response response = given().spec(requestSpec)
			.body(registerRequest)
			.when()
			.post("/auth/register")
			.then()
			.extract()
			.response();

		log.info("注册请求完成，状态码：{}", response.getStatusCode());
		return response;
	}

	/**
	 * 使用当前 Token 创建已鉴权的请求
	 * @return 已配置 Authorization 的请求规范
	 */
	protected RequestSpecification authenticatedRequest() {
		return given().spec(requestSpec).header("Authorization", "Bearer " + accessToken);
	}

	// ==================== 通用断言方法 ====================

	/**
	 * 断言响应成功（HTTP 2xx）
	 */
	protected void assertSuccess(Response response) {
		int statusCode = response.getStatusCode();
		assertTrue(statusCode >= 200 && statusCode < 300,
				String.format("期望响应成功，但状态码为 %d，响应体：%s", statusCode, response.getBody().asString()));
	}

	/**
	 * 断言响应失败（HTTP 4xx 或 5xx）
	 */
	protected void assertFailure(Response response) {
		int statusCode = response.getStatusCode();
		assertTrue(statusCode >= 400, String.format("期望响应失败，但状态码为 %d", statusCode));
	}

	/**
	 * 断言 JSON 字段存在
	 */
	protected void assertFieldExists(Response response, String fieldPath) {
		Object value = response.jsonPath().get(fieldPath);
		assertNotNull(value, String.format("字段 '%s' 不存在", fieldPath));
	}

	/**
	 * 断言 JSON 字段值
	 */
	protected void assertFieldEquals(Response response, String fieldPath, Object expectedValue) {
		Object actualValue = response.jsonPath().get(fieldPath);
		assertEquals(expectedValue, actualValue, String.format("字段 '%s' 的值不匹配", fieldPath));
	}

	// ==================== 轮询查询工具 ====================

	/**
	 * 轮询查询直到条件满足
	 * @param pollAction 轮询动作（返回 Response）
	 * @param condition 条件判断（返回 true 表示满足）
	 * @param message 超时错误消息
	 * @return 最后一次查询的响应
	 */
	protected Response pollUntil(java.util.function.Supplier<Response> pollAction,
			java.util.function.Predicate<Response> condition, String message) {
		await().atMost(Duration.ofSeconds(maxWaitSeconds))
			.pollInterval(pollIntervalMillis, TimeUnit.MILLISECONDS)
			.until(() -> {
				Response response = pollAction.get();
				boolean result = condition.test(response);
				if (!result) {
					log.info("轮询条件未满足，继续等待...");
				}
				return result;
			});

		// 再次执行以获取最终结果
		Response finalResponse = pollAction.get();
		assertTrue(condition.test(finalResponse), message);
		return finalResponse;
	}

	// ==================== 工具方法 ====================

	/**
	 * 获取环境变量（带默认值）
	 */
	private static String getEnv(String key, String defaultValue) {
		String value = System.getenv(key);
		return StringUtils.isNotBlank(value) ? value : defaultValue;
	}

	/**
	 * 获取环境变量（整数）
	 */
	private static int getEnvInt(String key, int defaultValue) {
		String value = System.getenv(key);
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			log.warn("环境变量 {} 的值 '{}' 不是有效整数，使用默认值 {}", key, value, defaultValue);
			return defaultValue;
		}
	}

	/**
	 * 脱敏 Token
	 */
	private String maskToken(String token) {
		if (token == null || token.length() < 10) {
			return "***";
		}
		return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
	}

	/**
	 * 获取当前测试名称
	 */
	private String getTestName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * 延迟执行（用于调试）
	 */
	protected void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// ==================== 订单请求构建工具 ====================

	/**
	 * 构建创建订单请求（完整版）
	 * @param marketId 市场 ID
	 * @param outcome 订单结果（YES/NO）
	 * @param side 订单方向
	 * @param type 订单类型
	 * @param quantity 数量
	 * @param price 价格（LIMIT 订单必填，MARKET 订单可为空）
	 * @param timeInForce 有效期类型（可选，默认 GTC）
	 * @param idempotencyKey 幂等键
	 * @return CreateOrderRequest DTO
	 */
	protected CreateOrderRequest buildCreateOrderRequest(Long marketId, Outcome outcome, Side side, OrderType type,
			java.math.BigDecimal quantity, java.math.BigDecimal price, TimeInForce timeInForce, String idempotencyKey) {

		CreateOrderRequest request = new CreateOrderRequest();
		request.setMarketId(marketId);
		request.setOutcome(outcome);
		request.setSide(side);
		request.setType(type);
		request.setQuantity(quantity);
		request.setPrice(price);
		request.setTimeInForce(timeInForce);
		request.setIdempotencyKey(idempotencyKey);

		return request;
	}

	/**
	 * 构建创建限价订单请求（简化版，默认 YES）
	 * @param marketId 市场 ID
	 * @param side 订单方向
	 * @param quantity 数量
	 * @param price 价格
	 * @return CreateOrderRequest DTO
	 */
	protected CreateOrderRequest buildLimitOrderRequest(Long marketId, Side side, java.math.BigDecimal quantity,
			java.math.BigDecimal price) {

		return buildCreateOrderRequest(marketId, Outcome.YES, // 默认使用 YES
				side, OrderType.LIMIT, quantity, price, TimeInForce.GTC, java.util.UUID.randomUUID().toString());
	}

	/**
	 * 构建创建限价订单请求（完整版，支持指定 Outcome）
	 * @param marketId 市场 ID
	 * @param outcome 订单结果（YES/NO）
	 * @param side 订单方向
	 * @param quantity 数量
	 * @param price 价格
	 * @return CreateOrderRequest DTO
	 */
	protected CreateOrderRequest buildLimitOrderRequest(Long marketId, Outcome outcome, Side side,
			java.math.BigDecimal quantity, java.math.BigDecimal price) {

		return buildCreateOrderRequest(marketId, outcome, side, OrderType.LIMIT, quantity, price, TimeInForce.GTC,
				java.util.UUID.randomUUID().toString());
	}

	/**
	 * 构建创建市价订单请求（简化版，默认 YES）
	 * @param marketId 市场 ID
	 * @param side 订单方向
	 * @param quantity 数量
	 * @return CreateOrderRequest DTO
	 */
	protected CreateOrderRequest buildMarketOrderRequest(Long marketId, Side side, java.math.BigDecimal quantity) {

		return buildCreateOrderRequest(marketId, Outcome.YES, // 默认使用 YES
				side, OrderType.MARKET, quantity, null, // 市价单无需价格
				TimeInForce.IOC, // 市价单通常使用 IOC
				java.util.UUID.randomUUID().toString());
	}

	/**
	 * 构建创建市价订单请求（完整版，支持指定 Outcome）
	 * @param marketId 市场 ID
	 * @param outcome 订单结果（YES/NO）
	 * @param side 订单方向
	 * @param quantity 数量
	 * @return CreateOrderRequest DTO
	 */
	protected CreateOrderRequest buildMarketOrderRequest(Long marketId, Outcome outcome, Side side,
			java.math.BigDecimal quantity) {

		return buildCreateOrderRequest(marketId, outcome, side, OrderType.MARKET, quantity, null, // 市价单无需价格
				TimeInForce.IOC, // 市价单通常使用 IOC
				java.util.UUID.randomUUID().toString());
	}

	// ==================== Market 相关方法 ====================

	/**
	 * 为每个测试用例创建一个新的市场 确保测试之间的隔离性
	 */
	protected void createNewMarketForTest() {
		log.info("为当前测试用例创建新的市场");
		try {
			Long createdMarketId = createMarket();
			TEST_MARKET_ID = createdMarketId;
			waitForMarketReady(TEST_MARKET_ID);
			log.info("Market 创建成功，ID: {}，测试将使用此 Market", TEST_MARKET_ID);
		}
		catch (Exception e) {
			log.error("创建 Market 失败", e);
			throw new IllegalStateException(
					"无法创建测试所需的 Market。请确保：\n" + "1. 用户具有 market_create 权限\n" + "2. 数据库连接正常\n" + "3. Market 服务运行正常", e);
		}
	}

	/**
	 * 确保测试所需的 Market 存在 策略： 1. 先查询 Market ID 1 是否存在且有效 2. 如果不存在，查询有效的 Market 列表 3.
	 * 如果列表为空，创建一个新的 Market 4. 更新 TEST_MARKET_ID
	 * @deprecated 使用 {@link #createNewMarketForTest()} 代替，确保测试隔离性
	 */
	@Deprecated
	protected void ensureMarketExists() {
		log.info("开始检查测试所需的 Market");

		// 策略1: 先查询 Market ID 是否存在
		Response queryResponse = authenticatedRequest().when()
			.get("/order/market/" + TEST_MARKET_ID)
			.then()
			.extract()
			.response();

		if (queryResponse.getStatusCode() == 200) {
			Object data = queryResponse.jsonPath().get("data");
			if (data != null) {
				// 检查Market状态是否为ACTIVE
				Object statusObj = queryResponse.jsonPath().get("data.status");
				if (statusObj != null) {
					String status = statusObj.toString();
					if ("ACTIVE".equals(status)) {
						if (isMarketSymbolsReady(queryResponse)) {
							log.info("Market ID {} 已存在且为有效状态", TEST_MARKET_ID);
							return;
						}
						log.info("Market ID {} 已激活但 symbol 未就绪，等待异步准备", TEST_MARKET_ID);
						waitForMarketReady(TEST_MARKET_ID);
						return;
					}
				}
			}
		}

		log.info("Market ID {} 不存在或无效，尝试查询有效的 Market 列表", TEST_MARKET_ID);

		// 策略2: 查询有效的 Market 列表
		Response activeMarketsResponse = authenticatedRequest().when()
			.get("/order/market/active")
			.then()
			.extract()
			.response();

		if (activeMarketsResponse.getStatusCode() == 200) {
			java.util.List<Map<String, Object>> markets = activeMarketsResponse.jsonPath().getList("data");
			if (markets != null && !markets.isEmpty()) {
				// 使用第一个有效的Market
				// MarketDTO 使用 marketId 字段（驼峰命名）
				Object marketIdObj = markets.get(0).get("marketId");
				if (marketIdObj == null) {
					// 兼容旧格式，尝试使用 id 字段
					marketIdObj = markets.get(0).get("id");
				}
				if (marketIdObj == null) {
					log.error("Market 数据中未找到 marketId 或 id 字段，Market 数据: {}", markets.get(0));
					throw new IllegalStateException("Market 数据格式不正确，缺少 marketId 字段");
				}
				Long marketId = ((Number) marketIdObj).longValue();
				TEST_MARKET_ID = marketId;
				log.info("使用已存在的有效 Market，ID: {}", TEST_MARKET_ID);
				waitForMarketReady(TEST_MARKET_ID);
				return;
			}
		}

		// 策略3: 没有有效的Market，尝试创建一个新的
		log.info("没有找到有效的 Market，尝试创建新的 Market");
		try {
			Long createdMarketId = createMarket();
			TEST_MARKET_ID = createdMarketId;
			waitForMarketReady(TEST_MARKET_ID);
			log.info("Market 创建成功，ID: {}，测试将使用此 Market", TEST_MARKET_ID);
		}
		catch (Exception e) {
			log.error("创建 Market 失败，可能是权限问题", e);
			throw new IllegalStateException("无法创建测试所需的 Market。请确保：\n" + "1. 用户具有 market_create 权限\n"
					+ "2. 或手动在数据库中创建一个有效的 Market\n" + "3. 或在数据库初始化脚本中添加 Market 数据", e);
		}
	}

	/**
	 * 创建Market
	 * @return 创建的Market ID
	 */
	private Long createMarket() {
		CreateMarketRequest request = new CreateMarketRequest();
		request.setName("E2E测试市场-" + System.currentTimeMillis());
		request.setStatus(MarketStatus.ACTIVE);
		request.setExpireAt(System.currentTimeMillis() + 86400L * 365 * 1000); // 1年后过期（Unix时间戳，毫秒）

		Response response = authenticatedRequest().contentType(ContentType.JSON)
			.accept(ContentType.JSON)
			.body(request)
			.when()
			.post("/order/market")
			.then()
			.extract()
			.response();

		if (response.getStatusCode() == 200) {
			Object data = response.jsonPath().get("data");
			if (data != null) {
				Long createdMarketId = ((Number) data).longValue();
				log.info("Market 创建成功，ID: {}", createdMarketId);
				return createdMarketId;
			}
		}

		String errorMsg = response.getBody().asString();
		log.error("创建 Market 失败，状态码: {}, 响应: {}", response.getStatusCode(), errorMsg);
		throw new IllegalStateException("创建 Market 失败: " + errorMsg);
	}

	private void waitForMarketReady(Long marketId) {
		pollUntil(() -> authenticatedRequest().when().get("/order/market/" + marketId).then().extract().response(),
				this::isMarketSymbolsReady, "Market 未就绪：状态不为 ACTIVE 或 symbol 未就绪");
	}

	private boolean isMarketSymbolsReady(Response response) {
		if (response.getStatusCode() != 200 || response.jsonPath().get("data") == null) {
			return false;
		}
		Object statusObj = response.jsonPath().get("data.status");
		if (statusObj == null) {
			return false;
		}
		String status = statusObj.toString();
		if (!"ACTIVE".equals(status)) {
			return false;
		}
		return response.jsonPath().get("data.symbolIdYes") != null
				&& response.jsonPath().get("data.symbolIdNo") != null;
	}

	/**
	 * 查询Market详情
	 * @param marketId 市场 ID
	 * @return 响应
	 */
	protected Response getMarket(Long marketId) {
		return authenticatedRequest().when().get("/order/market/" + marketId).then().extract().response();
	}

	// ==================== Vault（金库）相关方法 ====================

	/**
	 * 查询账户余额
	 * @param accountId 账户 ID
	 * @param symbol 资产符号（如 USDC）
	 * @return 余额（如果不存在返回 0）
	 */
	protected BigDecimal getBalance(Long accountId, String symbol) {
		Response response = authenticatedRequest().queryParam("accountId", accountId)
			.queryParam("symbol", symbol)
			.when()
			.get("/vault/balance")
			.then()
			.extract()
			.response();

		if (response.getStatusCode() == 200) {
			Object data = response.jsonPath().get("data");
			if (data != null) {
				Object availableObj = response.jsonPath().get("data.available");
				if (availableObj != null) {
					return new BigDecimal(availableObj.toString());
				}
			}
		}

		log.warn("无法获取余额，accountId={}, symbol={}, 返回 0", accountId, symbol);
		return BigDecimal.ZERO;
	}

	/**
	 * 检查资产是否存在，不存在则创建
	 * @param symbol 资产符号
	 * @param decimals 小数位数
	 */
	protected void ensureAssetExists(String symbol, Integer decimals) {
		log.info("检查资产是否存在：symbol={}", symbol);

		// 1. 尝试查询资产
		Response getResponse = authenticatedRequest().when()
			.get("/vault/asset/symbol/" + symbol)
			.then()
			.extract()
			.response();

		// 2. 如果资产存在，直接返回
		if (getResponse.getStatusCode() == 200) {
			Integer code = getResponse.jsonPath().getInt("code");
			if (code != null && code == 0) {
				log.info("资产已存在：symbol={}", symbol);
				return;
			}
		}

		// 3. 资产不存在，创建新资产
		log.info("资产不存在，开始创建：symbol={}, decimals={}", symbol, decimals);

		Map<String, Object> createRequest = new HashMap<>();
		createRequest.put("symbol", symbol);
		createRequest.put("decimals", decimals);
		createRequest.put("isActive", true);

		Response createResponse = authenticatedRequest().body(createRequest)
			.when()
			.post("/vault/asset")
			.then()
			.extract()
			.response();

		if (createResponse.getStatusCode() == 200) {
			assertSuccess(createResponse);
			log.info("资产创建成功：symbol={}, decimals={}", symbol, decimals);
		}
		else {
			String errorMsg = createResponse.getBody().asString();
			log.error("资产创建失败，状态码: {}, 响应: {}", createResponse.getStatusCode(), errorMsg);
			throw new IllegalStateException("资产创建失败: " + errorMsg);
		}
	}

	/**
	 * 充值（增加账户余额） 在充值前会自动检查并创建资产（如果不存在）
	 * @param accountId 账户 ID
	 * @param symbol 资产符号
	 * @param amount 充值金额
	 * @return 充值后的余额
	 */
	protected BigDecimal deposit(Long userId, String symbol, BigDecimal amount) {
		log.info("开始充值：userId={}, symbol={}, amount={}", userId, symbol, amount);

		// 在充值前确保资产存在（默认使用 6 位小数，适用于 USDC 等稳定币）
		ensureAssetExists(symbol, 6);

		DepositRequest request = new DepositRequest();
		request.setUserId(userId);
		request.setSymbol(symbol);
		request.setAmount(amount);
		request.setRefId("e2e-deposit-" + System.currentTimeMillis());

		Response response = authenticatedRequest().body(request)
			.when()
			.post("/vault/deposit")
			.then()
			.extract()
			.response();

		if (response.getStatusCode() == 200) {
			assertSuccess(response);
			Object newBalanceObj = response.jsonPath().get("data.available");

			if (newBalanceObj != null) {
				BigDecimal newBalance = new BigDecimal(newBalanceObj.toString());
				log.info("充值成功，新余额: {}", newBalance);
				return newBalance;
			}
		}

		String errorMsg = response.getBody().asString();
		log.error("充值失败，状态码: {}, 响应: {}", response.getStatusCode(), errorMsg);
		throw new IllegalStateException("充值失败: " + errorMsg);
	}

	/**
	 * 确保账户有足够余额（不够则充值）
	 * @param accountId 账户 ID
	 * @param symbol 资产符号
	 * @param requiredAmount 所需金额
	 * @return 充值后的余额
	 */
	protected BigDecimal ensureSufficientBalance(Long userId, String symbol, BigDecimal requiredAmount) {
		log.info("检查账户余额是否充足：userId={}, symbol={}, 所需金额={}", userId, symbol, requiredAmount);

		// 查询当前余额
		BigDecimal currentBalance = getBalance(userId, symbol);
		log.info("当前余额: {}", currentBalance);

		// 如果余额充足，直接返回
		if (currentBalance.compareTo(requiredAmount) >= 0) {
			log.info("余额充足，无需充值");
			return currentBalance;
		}

		// 计算需要充值的金额（多充值一些，避免边界问题）
		BigDecimal shortfall = requiredAmount.subtract(currentBalance);
		BigDecimal depositAmount = shortfall.multiply(BigDecimal.valueOf(1.5)); // 多充值 50%
		log.info("余额不足，需要充值: {}", depositAmount);

		// 执行充值
		return deposit(userId, symbol, depositAmount);
	}

	/**
	 * 计算订单所需的资金
	 * @param side 订单方向
	 * @param quantity 数量
	 * @param price 价格（如果是市价单可为 null）
	 * @return 所需资金
	 */
	protected BigDecimal calculateRequiredFunds(Side side, BigDecimal quantity, BigDecimal price) {
		if (side == Side.BUY) {
			// 买单需要支付 quantity * price 的资金
			if (price != null) {
				return quantity.multiply(price);
			}
			else {
				// 市价买单，按最大可能价格估算（这里简单估算为一个较大值）
				return quantity.multiply(BigDecimal.valueOf(100000)); // 预估一个较大值
			}
		}
		else {
			// 卖单需要持有相应数量的资产（这里简化处理，暂不实现卖单的资产检查）
			return BigDecimal.ZERO;
		}
	}

	/**
	 * 确保账户有足够的 outcome 资产（用于 SELL 订单）
	 * @param accountId 账户 ID
	 * @param marketId 市场 ID
	 * @param outcome 结果（YES/NO）
	 * @param requiredAmount 所需数量
	 * @return 充值后的余额
	 */
	protected BigDecimal ensureSufficientOutcomeAsset(Long userId, Long marketId, Outcome outcome,
			BigDecimal requiredAmount) {
		// 构建 outcome 资产符号：格式 "M{marketId}_{outcome}"，例如 "M1_YES" 或 "M1_NO"
		// 必须与 MarketCreatedEventHandler 和 OrderServiceImpl 中的格式保持一致
		String assetSymbol = String.format("M%d_%s", marketId, outcome.name());
		log.info("检查 outcome 资产余额：userId={}, symbol={}, 所需数量={}", userId, assetSymbol, requiredAmount);

		// 查询当前余额
		BigDecimal currentBalance = getBalance(userId, assetSymbol);
		log.info("当前 {} 资产余额: {}", assetSymbol, currentBalance);

		// 如果余额充足，直接返回
		if (currentBalance.compareTo(requiredAmount) >= 0) {
			log.info("outcome 资产余额充足，无需充值");
			return currentBalance;
		}

		// 计算需要充值的数量（多充值一些，避免边界问题）
		BigDecimal shortfall = requiredAmount.subtract(currentBalance);
		BigDecimal depositAmount = shortfall.multiply(BigDecimal.valueOf(1.5)); // 多充值 50%
		log.info("outcome 资产余额不足，需要充值: {}", depositAmount);

		// 执行充值
		return deposit(userId, assetSymbol, depositAmount);
	}

}
