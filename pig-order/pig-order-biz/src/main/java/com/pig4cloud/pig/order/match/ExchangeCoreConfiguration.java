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
import exchange.core2.core.common.api.ApiAddUser;
import exchange.core2.core.common.api.ApiAdjustUserBalance;
import exchange.core2.core.common.config.ExchangeConfiguration;
import exchange.core2.core.common.config.InitialStateConfiguration;
import exchange.core2.core.common.config.OrdersProcessingConfiguration;
import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.match.MatchingEngineSymbolService;
import com.pig4cloud.pig.order.match.event.ExchangeCoreInitedEvent;
import com.pig4cloud.pig.order.service.MarketService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

		// Get default asset symbol name from mapping (e.g., 1 -> "USDC")
		String defaultAssetSymbol = matchingEngineProperties.getAssetSymbol(matchingEngineProperties.getDefaultAsset());

		InitialStateConfiguration initStateCfg = InitialStateConfiguration.cleanStart(defaultAssetSymbol);

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

		log.info("ExchangeCore created with default asset: {} ({}), risk mode: {}",
				matchingEngineProperties.getDefaultAsset(), defaultAssetSymbol,
				ordersProcessingCfg.getRiskProcessingMode());

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

	@Bean
	public ExchangeCoreInitializer exchangeCoreInitializer(MarketService marketService,
			MatchingEngineSymbolService matchingEngineSymbolService, ExchangeApi exchangeApi,
			MatchingEngineProperties matchingEngineProperties, ApplicationEventPublisher eventPublisher) {
		return new ExchangeCoreInitializer(marketService, matchingEngineSymbolService, exchangeApi,
				matchingEngineProperties, eventPublisher);
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

	/**
	 * Initializes exchange-core with system user and active markets.
	 */
	@Slf4j
	@RequiredArgsConstructor
	static class ExchangeCoreInitializer {

		private static final long SYSTEM_UID = 0L;

		private final AtomicBoolean initialized = new AtomicBoolean(false);

		private final MarketService marketService;

		private final MatchingEngineSymbolService matchingEngineSymbolService;

		private final ExchangeApi exchangeApi;

		private final MatchingEngineProperties matchingEngineProperties;

		private final ApplicationEventPublisher eventPublisher;

		@EventListener(ApplicationReadyEvent.class)
		public void initialize(ApplicationReadyEvent event) {
			if (!initialized.compareAndSet(false, true)) {
				return;
			}

			initSystemUser();

			List<Market> activeMarkets = marketService.listActiveMarkets(java.time.Instant.now());
			for (Market market : activeMarkets) {
				matchingEngineSymbolService.ensureSymbol(market.getMarketId().intValue());
			}
			log.info("Registered {} active markets in matching engine", activeMarkets.size());

			eventPublisher.publishEvent(new ExchangeCoreInitedEvent());
		}

		private void initSystemUser() {
			CommandResultCode addUserResult = exchangeApi
				.submitCommandAsync(ApiAddUser.builder().uid(SYSTEM_UID).build())
				.join();
			if (addUserResult != CommandResultCode.SUCCESS
					&& addUserResult != CommandResultCode.USER_MGMT_USER_ALREADY_EXISTS) {
				throw new IllegalStateException("Failed to register system user: " + addUserResult);
			}

			// TODO: Replace with real balance provisioning for the system user.
			long maxBalance = Long.MAX_VALUE / 4;
			CommandResultCode balanceResult = exchangeApi
				.submitCommandAsync(ApiAdjustUserBalance.builder()
					.uid(SYSTEM_UID)
					.currency(matchingEngineProperties.getDefaultAsset())
					.amount(maxBalance)
					.transactionId(SYSTEM_UID + 1) // default transactionId is 0. avoid
													// duplicate transactionId
					.build())
				.join();

			if (balanceResult != CommandResultCode.SUCCESS) {
				throw new IllegalStateException("Failed to fund system user: " + balanceResult);
			}
		}

	}

}
