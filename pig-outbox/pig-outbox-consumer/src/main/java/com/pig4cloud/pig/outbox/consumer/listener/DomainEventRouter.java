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

package com.pig4cloud.pig.outbox.consumer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry.HandlerMethod;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 领域事件路由器
 * <p>
 * 将 Kafka 消息路由到已注册的事件处理器
 *
 * @author pig4cloud
 * @date 2025-01-21
 */
@Slf4j
@RequiredArgsConstructor
public class DomainEventRouter {

	private final EventHandlerRegistry eventHandlerRegistry;

	private final ObjectMapper objectMapper;

	/**
	 * 路由 Kafka 消息到已注册的处理器
	 * @param messageValue Kafka 消息值（JSON 格式的 DomainEventEnvelope）
	 */
	public void route(String messageValue) {
		try {
			// 1. 反序列化为 DomainEventEnvelope
			DomainEventEnvelope<?> envelope = objectMapper.readValue(messageValue, DomainEventEnvelope.class);
			validateEnvelope(envelope);

			// 2. 查找已注册的处理器
			List<HandlerMethod> handlers = eventHandlerRegistry.getHandlers(envelope.domain(), envelope.eventType());

			if (handlers.isEmpty()) {
				log.warn("No handler registered for event: domain={}, eventType={}, eventId={}", envelope.domain(),
						envelope.eventType(), envelope.eventId());
				return;
			}

			// 3. 依次调用所有处理器
			for (HandlerMethod handler : handlers) {
				invokeHandler(handler, envelope);
			}
		}
		catch (Exception e) {
			log.error("Failed to route event: message={}", messageValue, e);
			throw new RuntimeException("Event routing failed", e);
		}
	}

	/**
	 * 调用单个事件处理器
	 */
	private void invokeHandler(HandlerMethod handler, DomainEventEnvelope<?> envelope) {
		try {
			// 设置可访问性以支持内部类和非公共方法
			handler.getMethod().setAccessible(true);
			handler.getMethod().invoke(handler.getBean(), envelope);

			log.debug("Handler invoked successfully: eventId={}, handler={}.{}", envelope.eventId(),
					handler.getBean().getClass().getSimpleName(), handler.getMethod().getName());
		}
		catch (Exception e) {
			log.error("Handler invocation failed: eventId={}, handler={}.{}", envelope.eventId(),
					handler.getBean().getClass().getSimpleName(), handler.getMethod().getName(), e);
			throw new RuntimeException("Handler failed", e);
		}
	}

	private void validateEnvelope(DomainEventEnvelope<?> envelope) {
		if (envelope.domain() == null || envelope.domain().isBlank() || envelope.eventType() == null
				|| envelope.eventType().isBlank() || envelope.aggregateId() == null || envelope.aggregateId().isBlank()) {
			throw new IllegalArgumentException("Missing required event metadata: domain/eventType/aggregateId");
		}
	}

}
