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

import com.pig4cloud.pig.outbox.consumer.config.OutboxConsumerProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 领域事件 Kafka 监听器
 * <p>
 * 监听 domain.* topic 并路由到事件处理器
 *
 * @author pig4cloud
 * @date 2025-01-21
 */
@Slf4j
@RequiredArgsConstructor
public class DomainEventKafkaListener {

	private final OutboxConsumerProperties consumerProperties;

	private final DomainEventRouter router;

	@PostConstruct
	public void init() {
		if (consumerProperties.getDomains().isEmpty()) {
			log.warn("No domains configured for Kafka consumer");
		}
		else {
			log.info("Kafka consumer will subscribe to domains: {}", consumerProperties.getDomains());
		}
	}

	/**
	 * 监听所有配置的领域 topic
	 * <p>
	 * Topic 模式: domain.*
	 */
	@KafkaListener(topicPattern = "domain\\..*", groupId = "${pig.outbox.consumer.group-id-prefix:pig-outbox-consumer}",
			containerFactory = "kafkaListenerContainerFactory")
	public void onDomainEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
		String topic = record.topic();
		String domain = extractDomainFromTopic(topic);

		// 过滤未配置的领域
		if (!consumerProperties.getDomains().contains(domain)) {
			log.trace("Ignoring event from unconfigured domain: {}", domain);
			acknowledgment.acknowledge();
			return;
		}

		log.debug("Received event: topic={}, partition={}, offset={}, key={}", topic, record.partition(),
				record.offset(), record.key());

		try {
			// 路由到处理器
			router.route(record.value());

			// 提交 offset（成功处理后）
			acknowledgment.acknowledge();
		}
		catch (Exception e) {
			log.error("Failed to process event: topic={}, partition={}, offset={}", topic, record.partition(),
					record.offset(), e);
			// 不提交 offset，将根据 Kafka 消费者配置重试
			throw e;
		}
	}

	/**
	 * 从 topic 名称中提取 domain
	 * @param topic 例如 "domain.order"
	 * @return 例如 "order"
	 */
	private String extractDomainFromTopic(String topic) {
		return topic.substring("domain.".length());
	}

}
