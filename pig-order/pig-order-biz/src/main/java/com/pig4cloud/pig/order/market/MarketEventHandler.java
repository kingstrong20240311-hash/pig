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

package com.pig4cloud.pig.order.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.match.MatchingEngineProperties;
import com.pig4cloud.pig.order.match.MatchingEngineSymbolService;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.market.MarketAssetsReadyPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Market event handler for async market provisioning.
 *
 * @author lengleng
 * @date 2026/01/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventHandler {

	private final MarketService marketService;

	private final MatchingEngineSymbolService matchingEngineSymbolService;

	private final MatchingEngineProperties matchingEngineProperties;

	private final ObjectMapper objectMapper;

	@DomainEventHandler(domain = "market", eventType = "MarketAssetsReady")
	@Transactional(rollbackFor = Exception.class)
	public void handleMarketAssetsReady(DomainEventEnvelope<MarketAssetsReadyPayload> event) {
		MarketAssetsReadyPayload payload = event.payloadAs(objectMapper, MarketAssetsReadyPayload.class);
		if (payload == null || payload.getMarketId() == null || payload.getCurrencyIdYes() == null
				|| payload.getCurrencyIdNo() == null) {
			throw new IllegalArgumentException("Invalid MarketAssetsReady payload: " + event.payloadJson());
		}

		Long marketId = payload.getMarketId();
		int symbolIdYes = payload.getCurrencyIdYes();
		int symbolIdNo = payload.getCurrencyIdNo();

		Market market = marketService.getMarket(marketId);
		if (market == null) {
			throw new IllegalArgumentException("Market not found: " + marketId);
		}

		marketService.activateMarketWithSymbols(marketId, symbolIdYes, symbolIdNo);

		int quoteCurrency = matchingEngineProperties.getDefaultAsset();
		matchingEngineSymbolService.ensureSymbol(symbolIdYes, symbolIdYes, quoteCurrency);
		matchingEngineSymbolService.ensureSymbol(symbolIdNo, symbolIdNo, quoteCurrency);

		log.info("Market assets ready handled: marketId={}, symbolIdYes={}, symbolIdNo={}", marketId, symbolIdYes,
				symbolIdNo);
	}

}
