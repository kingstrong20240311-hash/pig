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
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.config.OutboxKafkaProperties;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Kafka事件发布策略（微服务模式）
 * <p>
 * 将事件发送到Kafka消息队列
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaPublishStrategy implements EventPublishStrategy {

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final ObjectMapper objectMapper;

	private final OutboxKafkaProperties kafkaProperties;

	@Override
	public void publish(OutboxEvent event) {
		try {
			// 1. 构建 DomainEventEnvelope
			DomainEventEnvelope envelope = buildEnvelope(event);

			// 2. 计算 topic: "domain." + domain
			String topic = "domain." + event.getDomain();

			// 3. 序列化 envelope 为 JSON
			String messageValue = objectMapper.writeValueAsString(envelope);

			// 4. 同步发送到 Kafka (使用 partition_key 作为 Kafka key 保证同一聚合有序)
			SendResult<String, String> result = kafkaTemplate.send(topic, event.getPartitionKey(), messageValue)
				.get(kafkaProperties.getSendTimeoutSeconds(), TimeUnit.SECONDS);

			log.debug("Event published to Kafka: topic={}, partition={}, offset={}, eventId={}", topic,
					result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), event.getEventId());
		}
		catch (Exception e) {
			log.error("Failed to publish event to Kafka: eventId={}, domain={}, eventType={}", event.getEventId(),
					event.getDomain(), event.getEventType(), e);
			// 抛出异常以触发 OutboxEventDispatcher 的重试逻辑
			throw new RuntimeException("Kafka publish failed", e);
		}
	}

	/**
	 * 从 OutboxEvent 构建 DomainEventEnvelope
	 */
	private DomainEventEnvelope buildEnvelope(OutboxEvent event) {
		// 解析 headers JSON（如果存在）
		Map<String, String> headers = null;
		if (event.getHeadersJson() != null && !event.getHeadersJson().isEmpty()) {
			try {
				headers = objectMapper.readValue(event.getHeadersJson(), new TypeReference<Map<String, String>>() {
				});
			}
			catch (Exception e) {
				log.warn("Failed to deserialize event headers: eventId={}", event.getEventId(), e);
			}
		}

		return new DomainEventEnvelope(event.getEventId(), event.getDomain(), event.getAggregateType(),
				event.getAggregateId(), event.getEventType(), event.getCreatedAt(), headers, event.getPayloadJson());
	}

}
