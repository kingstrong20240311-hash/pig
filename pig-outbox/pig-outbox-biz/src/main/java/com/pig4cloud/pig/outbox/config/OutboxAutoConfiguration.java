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

package com.pig4cloud.pig.outbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.publisher.DomainEventPublisher;
import com.pig4cloud.pig.outbox.dispatcher.*;
import com.pig4cloud.pig.outbox.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.mapper.OutboxEventMapper;
import com.pig4cloud.pig.outbox.publisher.DbOutboxEventPublisher;
import com.pig4cloud.pig.outbox.publisher.InProcessEventPublisher;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Outbox自动配置
 * <p>
 * 根据配置的模式（单体/微服务）自动选择对应的Publisher实现和发布策略
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({ OutboxProperties.class, OutboxDispatcherProperties.class })
public class OutboxAutoConfiguration {

	private final OutboxProperties outboxProperties;

	/**
	 * 单体模式：数据库Outbox事件发布器
	 */
	@Bean
	@Primary
	@ConditionalOnProperty(prefix = "pig.outbox", name = "mode", havingValue = "monolithic")
	public DomainEventPublisher monolithicEventPublisher(OutboxEventService outboxEventService,
			ObjectMapper objectMapper) {
		log.info("Outbox configured in MONOLITHIC mode: using InProcessEventPublisher (saves to DB)");
		return new InProcessEventPublisher(outboxEventService, objectMapper);
	}

	/**
	 * 微服务模式：数据库Outbox事件发布器
	 */
	@Bean
	@Primary
	@ConditionalOnProperty(prefix = "pig.outbox", name = "mode", havingValue = "microservice", matchIfMissing = true)
	public DomainEventPublisher microserviceEventPublisher(OutboxEventService outboxEventService,
			ObjectMapper objectMapper) {
		log.info("Outbox configured in MICROSERVICE mode: using DbOutboxEventPublisher");
		return new DbOutboxEventPublisher(outboxEventService, objectMapper);
	}

	/**
	 * 单体模式：进程内事件发布策略
	 */
	@Bean
	@ConditionalOnProperty(prefix = "pig.outbox", name = "mode", havingValue = "monolithic")
	public EventPublishStrategy inProcessPublishStrategy(EventHandlerRegistry eventHandlerRegistry,
			ObjectMapper objectMapper) {
		log.info("Configuring InProcessPublishStrategy for event dispatching");
		return new InProcessPublishStrategy(eventHandlerRegistry, objectMapper);
	}

	/**
	 * 微服务模式：Kafka事件发布策略
	 */
	@Bean
	@ConditionalOnProperty(prefix = "pig.outbox", name = "mode", havingValue = "microservice", matchIfMissing = true)
	public EventPublishStrategy kafkaPublishStrategy() {
		log.info("Configuring KafkaPublishStrategy for event dispatching");
		return new KafkaPublishStrategy();
	}

	/**
	 * Outbox事件调度器
	 */
	@Bean
	public OutboxEventDispatcher outboxEventDispatcher(OutboxEventMapper outboxEventMapper,
			EventPublishStrategy publishStrategy, OutboxDispatcherProperties dispatcherProperties) {
		log.info("Configuring OutboxEventDispatcher with batchSize={}, enabled={}", dispatcherProperties.getBatchSize(),
				dispatcherProperties.isEnabled());
		return new OutboxEventDispatcher(outboxEventMapper, publishStrategy, dispatcherProperties);
	}

}
