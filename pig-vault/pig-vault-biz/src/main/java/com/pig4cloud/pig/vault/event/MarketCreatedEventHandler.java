/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pig.vault.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.market.MarketAssetsReadyPayload;
import com.pig4cloud.pig.outbox.api.payload.market.MarketCreatedPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.vault.api.entity.CurrencyIdSeq;
import com.pig4cloud.pig.vault.api.entity.VaultAsset;
import com.pig4cloud.pig.vault.mapper.CurrencyIdSeqMapper;
import com.pig4cloud.pig.vault.mapper.VaultAssetMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Handle MarketCreated events to provision YES/NO assets.
 *
 * @author pig4cloud
 * @date 2026-01-25
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pig.vault.market-event-handler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketCreatedEventHandler {

	private static final Logger log = LoggerFactory.getLogger(MarketCreatedEventHandler.class);

	private static final int DEFAULT_DECIMALS = 6;

	private final VaultAssetMapper vaultAssetMapper;

	private final CurrencyIdSeqMapper currencyIdSeqMapper;

	private final DomainEventPublisher domainEventPublisher;

	private final ObjectMapper objectMapper;

	@DomainEventHandler(domain = "market", eventType = "MarketCreated")
	@Transactional(rollbackFor = Exception.class)
	public void handleMarketCreated(DomainEventEnvelope<?> event) {
		MarketCreatedPayload payload = event.payloadAs(objectMapper, MarketCreatedPayload.class);
		Long marketId = payload == null ? null : payload.getMarketId();
		if (marketId == null) {
			marketId = toLong(extractAggregateId(event));
		}
		if (marketId == null) {
			throw new IllegalArgumentException("Invalid MarketCreated payload: " + extractPayloadJson(event));
		}

		String yesSymbol = buildOutcomeSymbol(marketId, "YES");
		String noSymbol = buildOutcomeSymbol(marketId, "NO");

		VaultAsset yesAsset = findOrCreateAsset(yesSymbol);
		VaultAsset noAsset = findOrCreateAsset(noSymbol);

		MarketAssetsReadyPayload readyPayload = new MarketAssetsReadyPayload(marketId, yesAsset.getCurrencyId(),
				noAsset.getCurrencyId());
		DomainEventEnvelope<MarketAssetsReadyPayload> readyEvent = new DomainEventEnvelope<>(
				UUID.randomUUID().toString(), "market", "Market", String.valueOf(marketId), "MarketAssetsReady",
				System.currentTimeMillis(), null, readyPayload);

		domainEventPublisher.publish(readyEvent);

		log.info("Market assets created: marketId={}, yesCurrencyId={}, noCurrencyId={}", marketId,
				yesAsset.getCurrencyId(), noAsset.getCurrencyId());
	}

	private VaultAsset findOrCreateAsset(String symbol) {
		VaultAsset asset = vaultAssetMapper
			.selectOne(Wrappers.<VaultAsset>lambdaQuery().eq(VaultAsset::getSymbol, symbol));
		if (asset == null) {
			asset = new VaultAsset();
			asset.setSymbol(symbol);
			asset.setDecimals(DEFAULT_DECIMALS);
			asset.setIsActive(true);
			asset.setCurrencyId(nextCurrencyId());
			vaultAssetMapper.insert(asset);
			return asset;
		}

		if (asset.getCurrencyId() == null) {
			asset.setCurrencyId(nextCurrencyId());
			vaultAssetMapper.updateById(asset);
		}
		return asset;
	}

	private int nextCurrencyId() {
		CurrencyIdSeq seq = new CurrencyIdSeq();
		currencyIdSeqMapper.insert(seq);
		Integer currentCurrencyId = seq.getId();
		if (currentCurrencyId == null) {
			throw new IllegalStateException("Failed to allocate currency id");
		}
		return currentCurrencyId;
	}

	private String buildOutcomeSymbol(Long marketId, String outcome) {
		return "M" + marketId + "_" + outcome;
	}

	private String extractPayloadJson(DomainEventEnvelope<?> event) {
		String payloadJson = toStringOrNull(invokeMethod(event, "payloadJson"));
		if (payloadJson != null) {
			return payloadJson;
		}
		return toStringOrNull(invokeMethod(event, "getPayloadJson"));
	}

	private String extractAggregateId(DomainEventEnvelope<?> event) {
		String aggregateId = toStringOrNull(invokeMethod(event, "aggregateId"));
		if (aggregateId != null) {
			return aggregateId;
		}
		return toStringOrNull(invokeMethod(event, "getAggregateId"));
	}

	private Object invokeMethod(Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		}
		catch (Exception e) {
			return null;
		}
	}

	private String toStringOrNull(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(value.toString());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

}
