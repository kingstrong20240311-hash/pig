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

package com.pig4cloud.pig.outbox.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 基于数据库Outbox的事件发布器（微服务模式）
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Slf4j
@RequiredArgsConstructor
public class DbOutboxEventPublisher implements DomainEventPublisher {

	private final OutboxEventService outboxEventService;

	private final ObjectMapper objectMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void publish(DomainEventEnvelope event) {
		OutboxEvent outboxEvent = new OutboxEvent();
		outboxEvent.setEventId(event.eventId());
		outboxEvent.setDomain(event.domain());
		outboxEvent.setAggregateType(event.aggregateType());
		outboxEvent.setAggregateId(event.aggregateId());
		outboxEvent.setEventType(event.eventType());
		outboxEvent.setPayloadJson(event.payloadJson());
		outboxEvent.setPartitionKey(event.aggregateId()); // 默认使用aggregateId作为分区键

		// 序列化headers
		if (event.headers() != null && !event.headers().isEmpty()) {
			try {
				outboxEvent.setHeadersJson(objectMapper.writeValueAsString(event.headers()));
			}
			catch (JsonProcessingException e) {
				log.warn("Failed to serialize event headers, eventId={}", event.eventId(), e);
			}
		}

		outboxEvent.setStatus(OutboxStatus.NEW);
		outboxEvent.setAttempts(0);
		outboxEvent.setCreatedAt(Instant.now());
		outboxEvent.setUpdatedAt(Instant.now());

		outboxEventService.save(outboxEvent);
		log.debug("Published event to outbox: eventId={}, domain={}, eventType={}", event.eventId(), event.domain(),
				event.eventType());
	}

}
