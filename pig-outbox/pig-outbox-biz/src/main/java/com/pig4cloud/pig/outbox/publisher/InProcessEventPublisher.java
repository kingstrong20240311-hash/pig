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
 * 进程内事件发布器（单体模式）
 * <p>
 * 先保存事件到数据库，后台由定时任务调度发布
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Slf4j
@RequiredArgsConstructor
public class InProcessEventPublisher implements DomainEventPublisher {

	private final OutboxEventService outboxEventService;

	private final ObjectMapper objectMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void publish(DomainEventEnvelope<?> event) {
		String payloadJson = resolvePayloadJson(event);

		OutboxEvent outboxEvent = new OutboxEvent();
		outboxEvent.setEventId(event.eventId());
		outboxEvent.setDomain(event.domain());
		outboxEvent.setAggregateType(event.aggregateType());
		outboxEvent.setAggregateId(event.aggregateId());
		outboxEvent.setEventType(event.eventType());
		outboxEvent.setPayloadJson(payloadJson);
		outboxEvent.setPartitionKey(event.aggregateId());

		// 序列化headers
		if (event.headers() != null && !event.headers().isEmpty()) {
			try {
				outboxEvent.setHeadersJson(objectMapper.writeValueAsString(event.headers()));
			}
			catch (JsonProcessingException e) {
				log.warn("Failed to serialize event headers, eventId={}", event.eventId(), e);
			}
		}

		outboxEvent.setStatus(OutboxStatus.PENDING);
		outboxEvent.setAttempts(0);
		outboxEvent.setCreatedAt(Instant.now());
		outboxEvent.setUpdatedAt(Instant.now());

		outboxEventService.save(outboxEvent);
		log.debug("Published event to outbox: eventId={}, domain={}, eventType={}", event.eventId(), event.domain(),
				event.eventType());
	}

	private String resolvePayloadJson(DomainEventEnvelope<?> event) {
		if (event.payloadJson() != null && !event.payloadJson().isBlank()) {
			return event.payloadJson();
		}
		if (event.payload() == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(event.payload());
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize event payload", e);
		}
	}

}
