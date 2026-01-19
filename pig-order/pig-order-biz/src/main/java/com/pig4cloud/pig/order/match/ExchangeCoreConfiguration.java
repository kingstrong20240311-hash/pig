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

import exchange.core2.core.ExchangeApi;
import exchange.core2.core.ExchangeCore;
import exchange.core2.core.IEventsHandler;
import exchange.core2.core.SimpleEventsProcessor;
import exchange.core2.core.common.config.ExchangeConfiguration;
import exchange.core2.core.common.config.InitialStateConfiguration;
import exchange.core2.core.common.config.OrdersProcessingConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ExchangeCore Configuration for Order Matching Engine
 *
 * @author lengleng
 * @date 2025/01/18
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MatchingEngineProperties.class)
public class ExchangeCoreConfiguration {

	private final MatchingEngineProperties matchingEngineProperties;

	@Bean
	public IEventsHandler orderEventsHandler(OrderMatchService orderMatchService) {
		return new OrderEventsHandler(orderMatchService);
	}

	@Bean
	public ExchangeCore exchangeCore(IEventsHandler eventsHandler) {
		// Create exchange configuration with Direct risk mode (no balance tracking)
		OrdersProcessingConfiguration ordersProcessingCfg = OrdersProcessingConfiguration.builder()
			.riskProcessingMode(OrdersProcessingConfiguration.RiskProcessingMode.NO_RISK_PROCESSING)
			.build();

		InitialStateConfiguration initStateCfg = InitialStateConfiguration
			.cleanStart(matchingEngineProperties.getDefaultAsset());

		ExchangeConfiguration exchangeConfiguration = ExchangeConfiguration.defaultBuilder()
			.ordersProcessingCfg(ordersProcessingCfg)
			.initStateCfg(initStateCfg)
			.build();

		// Create event processor with our custom handler
		SimpleEventsProcessor eventsProcessor = new SimpleEventsProcessor(eventsHandler);

		// Build exchange core
		ExchangeCore exchangeCore = ExchangeCore.builder()
			.resultsConsumer(eventsProcessor)
			.exchangeConfiguration(exchangeConfiguration)
			.build();

		log.info("ExchangeCore created with configuration: {}", exchangeConfiguration);

		return exchangeCore;
	}

	@Bean
	public ExchangeApi exchangeApi(ExchangeCore exchangeCore) {
		return exchangeCore.getApi();
	}

	/**
	 * Lifecycle bean to manage ExchangeCore startup and shutdown
	 */
	@Bean
	public ExchangeCoreLifecycle exchangeCoreLifecycle(ExchangeCore exchangeCore) {
		return new ExchangeCoreLifecycle(exchangeCore);
	}

	/**
	 * Manages ExchangeCore lifecycle with Spring application lifecycle
	 */
	@Slf4j
	@RequiredArgsConstructor
	static class ExchangeCoreLifecycle implements SmartLifecycle {

		private final ExchangeCore exchangeCore;

		private volatile boolean running = false;

		@Override
		public void start() {
			log.info("Starting ExchangeCore matching engine...");
			try {
				exchangeCore.startup();
				running = true;
				log.info("ExchangeCore matching engine started successfully");
			}
			catch (Exception e) {
				log.error("Failed to start ExchangeCore", e);
				throw new RuntimeException("Failed to start matching engine", e);
			}
		}

		@Override
		public void stop() {
			log.info("Stopping ExchangeCore matching engine...");
			try {
				exchangeCore.shutdown();
				running = false;
				log.info("ExchangeCore matching engine stopped successfully");
			}
			catch (Exception e) {
				log.error("Failed to stop ExchangeCore gracefully", e);
			}
		}

		@Override
		public boolean isRunning() {
			return running;
		}

		@Override
		public int getPhase() {
			// Start early, stop late
			return Integer.MIN_VALUE;
		}

	}

}
