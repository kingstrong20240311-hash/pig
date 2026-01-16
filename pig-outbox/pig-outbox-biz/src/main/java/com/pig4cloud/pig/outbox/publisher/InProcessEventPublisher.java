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

import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.outbox.handler.EventHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 进程内事件发布器（单体模式）
 * <p>
 * 直接在同一进程内同步调用事件处理器，无需经过数据库或消息队列
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Slf4j
@RequiredArgsConstructor
public class InProcessEventPublisher implements DomainEventPublisher {

	private final EventHandlerRegistry eventHandlerRegistry;

	@Override
	public void publish(DomainEventEnvelope event) {
		log.debug("Publishing event in-process: eventId={}, domain={}, eventType={}", event.eventId(), event.domain(),
				event.eventType());

		// 获取注册的处理器
		List<EventHandlerRegistry.HandlerMethod> handlers = eventHandlerRegistry.getHandlers(event.domain(),
				event.eventType());

		if (handlers.isEmpty()) {
			log.warn("No handler found for event: domain={}, eventType={}", event.domain(), event.eventType());
			return;
		}

		// 依次调用所有处理器
		for (EventHandlerRegistry.HandlerMethod handler : handlers) {
			try {
				handler.getMethod().invoke(handler.getBean(), event);
				log.debug("Event handled successfully: eventId={}, handler={}.{}", event.eventId(),
						handler.getBean().getClass().getSimpleName(), handler.getMethod().getName());
			}
			catch (Exception e) {
				log.error("Failed to handle event: eventId={}, handler={}.{}", event.eventId(),
						handler.getBean().getClass().getSimpleName(), handler.getMethod().getName(), e);
				// 单体模式下可以选择：继续处理其他handler，或者抛出异常中断事务
				// 这里选择继续处理，避免一个handler失败影响其他handler
			}
		}
	}

}
