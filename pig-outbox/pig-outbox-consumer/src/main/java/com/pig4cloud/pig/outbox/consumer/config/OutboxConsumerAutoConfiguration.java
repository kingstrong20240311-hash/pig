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

package com.pig4cloud.pig.outbox.consumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.consumer.listener.DomainEventKafkaListener;
import com.pig4cloud.pig.outbox.consumer.listener.DomainEventRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox Consumer 自动配置
 *
 * @author pig4cloud
 * @date 2025-01-21
 */
@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pig.outbox.consumer", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OutboxConsumerProperties.class)
public class OutboxConsumerAutoConfiguration {

	private final OutboxConsumerProperties consumerProperties;

	/**
	 * Kafka 消费者工厂
	 */
	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerProperties.getBootstrapServers());
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerProperties.isEnableAutoCommit());
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerProperties.getAutoOffsetReset());
		config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerProperties.getSessionTimeoutMs());
		config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerProperties.getMaxPollRecords());

		return new DefaultKafkaConsumerFactory<>(config);
	}

	/**
	 * Kafka 监听容器工厂
	 */
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory(
			ConsumerFactory<String, String> consumerFactory) {

		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setConcurrency(consumerProperties.getConcurrency());

		// 错误处理器：3 次重试，每次间隔 5 秒
		factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(5000L, 3)));

		// 手动提交模式以更好控制 offset
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

		log.info("Kafka consumer configured: domains={}, concurrency={}, bootstrap-servers={}",
				consumerProperties.getDomains(), consumerProperties.getConcurrency(),
				consumerProperties.getBootstrapServers());

		return factory;
	}

	/**
	 * 领域事件路由器
	 */
	@Bean
	public DomainEventRouter domainEventRouter(EventHandlerRegistry eventHandlerRegistry, ObjectMapper objectMapper) {
		return new DomainEventRouter(eventHandlerRegistry, objectMapper);
	}

		/**
	 * Kafka 监听器
	 */
		@Bean
		public DomainEventKafkaListener domainEventKafkaListener(
				OutboxConsumerProperties consumerProperties,
				DomainEventRouter domainEventRouter) {
			return new DomainEventKafkaListener(consumerProperties, domainEventRouter);
		}

}
