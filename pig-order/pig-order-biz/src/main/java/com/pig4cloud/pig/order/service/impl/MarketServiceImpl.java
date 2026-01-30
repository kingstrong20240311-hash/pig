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
import com.pig4cloud.pig.order.api.dto.CreateMarketRequest;
import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.api.entity.Order;
import com.pig4cloud.pig.order.api.enums.MarketStatus;
import com.pig4cloud.pig.order.api.enums.OrderStatus;
import com.pig4cloud.pig.order.mapper.MarketMapper;
import com.pig4cloud.pig.order.mapper.OrderMapper;
import com.pig4cloud.pig.outbox.api.payload.market.MarketCreatedPayload;
import com.pig4cloud.pig.order.service.MarketService;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderReducedPayload;
import com.pig4cloud.pig.order.event.MarketClosedPayload;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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

	private static final String DOMAIN_ORDER = "order";

	private static final String AGG_TYPE_ORDER = "Order";

	private static final String EVENT_ORDER_REDUCED = "OrderReduced";

	private static final String EVENT_MARKET_CREATED = "MarketCreated";

	private final MarketMapper marketMapper;

	private final OrderMapper orderMapper;

	private final OutboxEventService outboxEventService;

	private final DomainEventPublisher domainEventPublisher;

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
		if (market.getSymbolIdYes() == null || market.getSymbolIdNo() == null) {
			throw new IllegalStateException("Market symbols not ready: " + marketId);
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

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void activateMarketWithSymbols(Long marketId, int symbolIdYes, int symbolIdNo) {
		Market market = marketMapper.selectById(marketId);
		if (market == null) {
			throw new IllegalArgumentException("Market not found: " + marketId);
		}

		market.setSymbolIdYes(symbolIdYes);
		market.setSymbolIdNo(symbolIdNo);
		market.setStatus(MarketStatus.ACTIVE);
		market.setUpdateTime(Instant.now());
		marketMapper.updateById(market);

		log.info("Market activated with symbols: marketId={}, symbolIdYes={}, symbolIdNo={}", marketId, symbolIdYes,
				symbolIdNo);
	}

	private void expireOrdersForMarket(Long marketId) {
		List<Order> expiringOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
			.eq(Order::getMarketId, marketId)
			.in(Order::getStatus, OrderStatus.OPEN, OrderStatus.MATCHING, OrderStatus.PARTIALLY_FILLED,
					OrderStatus.CANCEL_REQUESTED));

		for (Order order : expiringOrders) {
			order.setStatus(OrderStatus.EXPIRED);
			orderMapper.updateById(order);
			publishOrderReducedEvent(order);
		}
	}

	private void publishMarketClosedEvent(Market market) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(IdUtil.randomUUID());
		event.setDomain(DOMAIN_MARKET);
		event.setAggregateType(AGG_TYPE_MARKET);
		event.setAggregateId(String.valueOf(market.getMarketId()));
		event.setEventType("MarketClosed");

		MarketClosedPayload payload = new MarketClosedPayload(market.getMarketId(), market.getExpireAt(),
				Instant.now());
		event.setPayloadJson(JSONUtil.toJsonStr(payload));

		event.setPartitionKey(String.valueOf(market.getMarketId()));
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());

		outboxEventService.save(event);
	}

	private void publishOrderReducedEvent(Order order) {
		// 资产金额：BUY = 剩余数量×价格，SELL = 剩余数量
		BigDecimal amount = order.getPrice() != null ? order.getRemainingQuantity().multiply(order.getPrice())
				: order.getRemainingQuantity();
		OrderReducedPayload payload = new OrderReducedPayload(order.getOrderId(), amount);

		DomainEventEnvelope<OrderReducedPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_ORDER, // domain
				AGG_TYPE_ORDER, // aggregateType
				String.valueOf(order.getOrderId()), // aggregateId
				EVENT_ORDER_REDUCED, // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	@Override
	public Long createMarket(CreateMarketRequest request) {
		Market market = new Market();
		market.setName(request.getName());
		market.setStatus(request.getStatus());
		// Convert Long timestamp (milliseconds) to Instant
		if (request.getExpireAt() != null) {
			market.setExpireAt(Instant.ofEpochMilli(request.getExpireAt()));
		}
		market.setCreateTime(Instant.now());
		market.setUpdateTime(Instant.now());

		marketMapper.insert(market);
		log.info("Created market: {}", market.getMarketId());

		publishMarketCreatedEvent(market);

		return market.getMarketId();
	}

	private void publishMarketCreatedEvent(Market market) {
		Long expireAtMillis = null;
		if (market.getExpireAt() != null) {
			expireAtMillis = market.getExpireAt().toEpochMilli();
		}
		MarketCreatedPayload payload = new MarketCreatedPayload(market.getMarketId(), market.getName(), expireAtMillis);

		DomainEventEnvelope<MarketCreatedPayload> event = new DomainEventEnvelope<>(IdUtil.randomUUID(), // eventId
				DOMAIN_MARKET, // domain
				AGG_TYPE_MARKET, // aggregateType
				String.valueOf(market.getMarketId()), // aggregateId
				EVENT_MARKET_CREATED, // eventType
				System.currentTimeMillis(), // occurredAt
				null, // headers
				payload // payload
		);

		domainEventPublisher.publish(event);
	}

	@Override
	public void updateMarketStatus(Long marketId, MarketStatus status) {
		Market market = marketMapper.selectById(marketId);
		if (market == null) {
			throw new IllegalArgumentException("Market not found: " + marketId);
		}

		market.setStatus(status);
		market.setUpdateTime(Instant.now());
		marketMapper.updateById(market);

		log.info("Updated market {} status to {}", marketId, status);
	}

	@Override
	public void deleteMarket(Long marketId) {
		Market market = marketMapper.selectById(marketId);
		if (market == null) {
			throw new IllegalArgumentException("Market not found: " + marketId);
		}

		market.setDelFlag("1");
		market.setUpdateTime(Instant.now());
		marketMapper.updateById(market);

		log.info("Deleted market: {}", marketId);
	}

}
