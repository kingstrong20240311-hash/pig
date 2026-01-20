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
import exchange.core2.core.common.CoreSymbolSpecification;
import exchange.core2.core.common.SymbolType;
import exchange.core2.core.common.api.binary.BatchAddSymbolsCommand;
import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers symbols in ExchangeCore on demand.
 *
 * @author lengleng
 * @date 2025/01/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngineSymbolService {

	private static final long DEFAULT_SCALE_K = 100L;

	private static final long DEFAULT_FEE = 0L;

	private final ExchangeApi exchangeApi;

	private final MatchingEngineProperties matchingEngineProperties;

	private final ConcurrentHashMap<Integer, Boolean> registeredSymbols = new ConcurrentHashMap<>();

	public void ensureSymbol(int symbolId) {
		if (registeredSymbols.putIfAbsent(symbolId, Boolean.TRUE) != null) {
			return;
		}

		CoreSymbolSpecification symbolSpec = CoreSymbolSpecification.builder()
			.symbolId(symbolId)
			.type(SymbolType.CURRENCY_EXCHANGE_PAIR)
			.baseCurrency(matchingEngineProperties.getDefaultAsset())
			.quoteCurrency(matchingEngineProperties.getDefaultAsset())
			.baseScaleK(DEFAULT_SCALE_K)
			.quoteScaleK(DEFAULT_SCALE_K)
			.takerFee(DEFAULT_FEE)
			.makerFee(DEFAULT_FEE)
			.marginBuy(0L)
			.marginSell(0L)
			.build();

		CommandResultCode resultCode = exchangeApi.submitBinaryDataAsync(new BatchAddSymbolsCommand(symbolSpec)).join();
		if (resultCode != CommandResultCode.SUCCESS) {
			registeredSymbols.remove(symbolId);
			throw new IllegalStateException("Failed to register symbol: " + symbolId + ", resultCode=" + resultCode);
		}

		log.info("Matching engine symbol registered: symbolId={}", symbolId);
	}

}
