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

package com.pig4cloud.pig.order.integration;

import exchange.core2.core.ExchangeApi;
import exchange.core2.core.common.api.reports.SingleUserReportQuery;
import exchange.core2.core.common.api.reports.SingleUserReportResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Order State Recovery Data is loaded BEFORE application starts
 * using MySQLContainer withInitScript
 *
 * @author lengleng
 * @date 2025/01/19
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = { "com.pig4cloud.pig.order", "com.pig4cloud.pig.outbox" })
@DisplayName("Order State Recovery Integration Tests")
class OrderStateRecoveryIntegrationTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("pig_order_test")
		.withUsername("test")
		.withPassword("test")
		.withInitScript("db/schema-and-recovery-data.sql");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		// Disable Nacos for tests
		registry.add("spring.cloud.nacos.discovery.enabled", () -> "false");
		registry.add("spring.cloud.nacos.config.enabled", () -> "false");
		registry.add("server.servlet.context-path", () -> "");
		// Enable state recovery
		registry.add("matching-engine.enable-state-recovery", () -> "true");
		// Asset symbol mapping
		registry.add("matching-engine.asset-symbols.1", () -> "USDC");
	}

	@Autowired
	private ExchangeApi exchangeApi;

	@MockBean
	private com.pig4cloud.pig.vault.api.feign.VaultService vaultService;

	/**
	 * I-07: Test recovery of MATCHING status orders on startup
	 */
	@Test
	@DisplayName("I-07: Recover MATCHING orders on startup")
	void testRecoverMatchingOrders() {
		// Given: Orders already inserted by withInitScript before app starts
		// Order 1000: MATCHING, remaining 10.00, user 100
		// Order 1001: MATCHING, remaining 5.00, user 100

		// When: Query user 100's orders from matching engine
		SingleUserReportQuery query = new SingleUserReportQuery(0L);
		CompletableFuture<SingleUserReportResult> future = exchangeApi.processReport(query, 0);
		SingleUserReportResult result = future.join();

		// Then: Verify both MATCHING orders are recovered in matching engine
		assertThat(result).isNotNull();
		assertThat(result.getOrders()).isNotNull();
		var ordersById = result.fetchIndexedOrders();

		// Order 1000: remaining 10.00 (1000 in scaled format)
		assertThat(ordersById.containsKey(1000L)).isTrue();
		assertThat(ordersById.get(1000L).getSize()).isEqualTo(1000L);

		// Order 1001: remaining 5.00 (500 in scaled format)
		assertThat(ordersById.containsKey(1001L)).isTrue();
		assertThat(ordersById.get(1001L).getSize()).isEqualTo(500L);
	}

	/**
	 * I-08: Test recovery of OPEN and PARTIALLY_FILLED orders on startup
	 */
	@Test
	@DisplayName("I-08: Recover OPEN/PARTIALLY_FILLED orders with remaining quantity")
	void testRecoverOpenAndPartiallyFilledOrders() {
		// Given: Orders already inserted by withInitScript before app starts
		// Order 2000: OPEN, remaining 10.00, user 100
		// Order 2001: PARTIALLY_FILLED, remaining 7.00, user 100

		// When: Query user 100's orders from matching engine
		SingleUserReportQuery query = new SingleUserReportQuery(0L);
		CompletableFuture<SingleUserReportResult> future = exchangeApi.processReport(query, 0);
		SingleUserReportResult result = future.join();

		// Then: Verify OPEN and PARTIALLY_FILLED orders are recovered
		assertThat(result).isNotNull();
		assertThat(result.getOrders()).isNotNull();
		var ordersById = result.fetchIndexedOrders();

		// Order 2000: OPEN, remaining 10.00 (1000 in scaled format)
		assertThat(ordersById.containsKey(2000L)).isTrue();
		assertThat(ordersById.get(2000L).getSize()).isEqualTo(1000L);

		// Order 2001: PARTIALLY_FILLED, remaining 7.00 (700 in scaled format)
		assertThat(ordersById.containsKey(2001L)).isTrue();
		assertThat(ordersById.get(2001L).getSize()).isEqualTo(700L);
	}

}
