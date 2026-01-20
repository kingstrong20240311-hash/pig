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

package com.pig4cloud.pig.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.MarketStatus;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.MarketMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Market Service Implementation
 *
 * @author lengleng
 * @date 2025/01/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

	private static final String DOMAIN_MARKET = "market";

	private static final String AGG_TYPE_MARKET = "Market";

	private final MarketMapper marketMapper;

	private final OrderMapper orderMapper;

	private final OutboxEventService outboxEventService;

	@Override
	public Market getMarket(Long marketId) {
		return marketMapper.selectById(marketId);
	}

	@Override
	public void assertMarketActive(Long marketId) {
		if (marketId == null) {
			throw new IllegalArgumentException("Market ID is required");
		}

		Market market = marketMapper.selectById(marketId);
		if (market == null) {
			throw new IllegalArgumentException("Market not found: " + marketId);
		}

		Instant now = Instant.now();
		if (market.getStatus() != MarketStatus.ACTIVE) {
			throw new IllegalStateException("Market is not active: " + marketId);
		}
		if (market.getExpireAt() != null && !market.getExpireAt().isAfter(now)) {
			throw new IllegalStateException("Market is expired: " + marketId);
		}
	}

	@Override
	public List<Market> listActiveMarkets(Instant now) {
		return marketMapper.selectList(Wrappers.<Market>lambdaQuery()
			.eq(Market::getStatus, MarketStatus.ACTIVE)
			.and(wrapper -> wrapper.isNull(Market::getExpireAt).or().gt(Market::getExpireAt, now)));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int expireDueMarkets(Instant now) {
		List<Market> expiredMarkets = marketMapper.selectList(Wrappers.<Market>lambdaQuery()
			.eq(Market::getStatus, MarketStatus.ACTIVE)
			.isNotNull(Market::getExpireAt)
			.le(Market::getExpireAt, now));

		if (expiredMarkets.isEmpty()) {
			return 0;
		}

		for (Market market : expiredMarkets) {
			market.setStatus(MarketStatus.EXPIRED);
			marketMapper.updateById(market);

			expireOrdersForMarket(market.getMarketId());
			publishMarketClosedEvent(market);
		}

		log.info("Expired {} markets at {}", expiredMarkets.size(), now);
		return expiredMarkets.size();
	}

	private void expireOrdersForMarket(Long marketId) {
		orderMapper.update(null,
				Wrappers.<Order>lambdaUpdate()
					.eq(Order::getMarketId, marketId)
					.in(Order::getStatus, OrderStatus.OPEN, OrderStatus.MATCHING, OrderStatus.PARTIALLY_FILLED,
							OrderStatus.CANCEL_REQUESTED)
					.set(Order::getStatus, OrderStatus.EXPIRED));
	}

	private void publishMarketClosedEvent(Market market) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(IdUtil.randomUUID());
		event.setDomain(DOMAIN_MARKET);
		event.setAggregateType(AGG_TYPE_MARKET);
		event.setAggregateId(String.valueOf(market.getMarketId()));
		event.setEventType("MarketClosed");

		Map<String, Object> payload = new HashMap<>();
		payload.put("marketId", market.getMarketId());
		payload.put("expireAt", market.getExpireAt());
		payload.put("closedAt", Instant.now());
		event.setPayloadJson(JSONUtil.toJsonStr(payload));

		event.setPartitionKey(String.valueOf(market.getMarketId()));
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());

		outboxEventService.save(event);
	}

}
