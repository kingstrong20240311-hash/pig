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

package com.pig4cloud.pig.outbox.dispatcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 进程内事件发布策略（单体模式）
 * <p>
 * 直接在同一进程内同步调用事件处理器
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
@Slf4j
@RequiredArgsConstructor
public class InProcessPublishStrategy implements EventPublishStrategy {

	private final EventHandlerRegistry eventHandlerRegistry;

	private final ObjectMapper objectMapper;

	@Override
	public void publish(OutboxEvent event) {
		// 构建DomainEventEnvelope
		DomainEventEnvelope envelope = buildEnvelope(event);

		// 获取注册的处理器
		List<EventHandlerRegistry.HandlerMethod> handlers = eventHandlerRegistry.getHandlers(event.getDomain(),
				event.getEventType());

		if (handlers.isEmpty()) {
			log.warn("No handler found for event: domain={}, eventType={}", event.getDomain(), event.getEventType());
			return;
		}

		// 依次调用所有处理器
		for (EventHandlerRegistry.HandlerMethod handler : handlers) {
			try {
				handler.getMethod().invoke(handler.getBean(), envelope);
				log.debug("Event handled successfully: eventId={}, handler={}.{}", event.getEventId(),
						handler.getBean().getClass().getSimpleName(), handler.getMethod().getName());
			}
			catch (Exception e) {
				log.error("Failed to handle event: eventId={}, handler={}.{}", event.getEventId(),
						handler.getBean().getClass().getSimpleName(), handler.getMethod().getName(), e);
				// 单体模式下可以选择：继续处理其他handler，或者抛出异常
				// 这里选择抛出异常，让调度器重试
				throw new RuntimeException("Failed to handle event in-process", e);
			}
		}
	}

	/**
	 * 从OutboxEvent构建DomainEventEnvelope
	 */
	private DomainEventEnvelope buildEnvelope(OutboxEvent event) {
		Map<String, String> headers = null;
		if (event.getHeadersJson() != null && !event.getHeadersJson().isEmpty()) {
			try {
				headers = objectMapper.readValue(event.getHeadersJson(), new TypeReference<Map<String, String>>() {
				});
			}
			catch (Exception e) {
				log.warn("Failed to deserialize event headers, eventId={}", event.getEventId(), e);
			}
		}

		return new DomainEventEnvelope(event.getEventId(), event.getDomain(), event.getAggregateType(),
				event.getAggregateId(), event.getEventType(), event.getCreatedAt().toEpochMilli(), headers, event.getPayloadJson());
	}

}
