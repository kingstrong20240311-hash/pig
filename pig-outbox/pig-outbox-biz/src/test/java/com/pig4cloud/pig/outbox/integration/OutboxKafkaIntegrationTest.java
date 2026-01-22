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

package com.pig4cloud.pig.outbox.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.dispatcher.OutboxEventDispatcher;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.mapper.OutboxEventMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Outbox Kafka 端到端集成测试 I-01: End-to-end publish - 启动 MySQL + Kafka 容器 - 插入 outbox
 * 事件到数据库 - 运行 dispatcher - 从 Kafka 消费 - 断言消息字段 (domain, eventType, aggregateId)
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@SpringBootTest
@Testcontainers
@DisplayName("I-01: End-to-end Outbox Kafka Integration")
class OutboxKafkaIntegrationTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
		.withDatabaseName("test_outbox")
		.withUsername("test")
		.withPassword("test")
		.withInitScript("schema.sql");

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// MySQL properties
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

		// Kafka properties
		registry.add("pig.outbox.kafka.bootstrap-servers", kafka::getBootstrapServers);

		// Outbox properties
		registry.add("pig.outbox.mode", () -> "microservice");
		registry.add("pig.outbox.dispatcher.enabled", () -> "true");
		registry.add("pig.outbox.dispatcher.batch-size", () -> "10");
	}

	@Autowired
	private OutboxEventMapper outboxEventMapper;

	@Autowired
	private OutboxEventDispatcher dispatcher;

	private ObjectMapper objectMapper;

	private static int testCounter = 0;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		// Clean up existing events
		outboxEventMapper.delete(null);

		// Increment test counter for unique IDs
		testCounter++;
	}

	@Test
	@DisplayName("I-01-001: End-to-end publish from DB to Kafka")
	void endToEnd_publish_from_db_to_kafka() throws Exception {
		// Given - Insert outbox event into database with unique ID
		String eventId = "test-evt-" + testCounter + "-001";
		String aggregateId = "order-" + testCounter + "-123";

		OutboxEvent event = new OutboxEvent();
		event.setEventId(eventId);
		event.setDomain("order");
		event.setAggregateType("Order");
		event.setAggregateId(aggregateId);
		event.setEventType("OrderCreated");
		event.setPayloadJson("{\"amount\":100,\"currency\":\"USD\"}");
		event.setHeadersJson("{\"userId\":\"user-1\",\"traceId\":\"trace-123\"}");
		event.setPartitionKey(aggregateId);
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());

		outboxEventMapper.insert(event);

		// When - Run dispatcher
		dispatcher.dispatch();

		// Then - Verify event status changed to SENT
		OutboxEvent updatedEvent = outboxEventMapper.selectById(event.getId());
		assertThat(updatedEvent.getStatus()).isEqualTo(OutboxStatus.SENT);

		// And - Consume message from Kafka and verify
		String topic = "domain.order";
		DomainEventEnvelope envelope = consumeAndFindEvent(topic, eventId);

		assertThat(envelope.eventId()).isEqualTo(eventId);
		assertThat(envelope.domain()).isEqualTo("order");
		assertThat(envelope.aggregateType()).isEqualTo("Order");
		assertThat(envelope.aggregateId()).isEqualTo(aggregateId);
		assertThat(envelope.eventType()).isEqualTo("OrderCreated");
		assertThat(envelope.payloadJson()).contains("amount");
		assertThat(envelope.payloadJson()).contains("currency");
		assertThat(envelope.headers()).containsEntry("userId", "user-1");
		assertThat(envelope.headers()).containsEntry("traceId", "trace-123");
	}

	@Test
	@DisplayName("I-01-002: Dispatcher handles multiple events in batch")
	void dispatcher_handles_multiple_events_in_batch() throws Exception {
		// Given - Insert multiple outbox events with unique IDs
		List<OutboxEvent> events = new ArrayList<>();
		Set<String> expectedEventIds = new HashSet<>();

		for (int i = 1; i <= 3; i++) {
			String eventId = "test-evt-" + testCounter + "-00" + i;
			expectedEventIds.add(eventId);

			OutboxEvent event = new OutboxEvent();
			event.setEventId(eventId);
			event.setDomain("order");
			event.setAggregateType("Order");
			event.setAggregateId("order-" + testCounter + "-" + i);
			event.setEventType("OrderCreated");
			event.setPayloadJson("{\"orderId\":" + i + "}");
			event.setPartitionKey("order-" + testCounter + "-" + i);
			event.setStatus(OutboxStatus.PENDING);
			event.setAttempts(0);
			event.setCreatedAt(Instant.now());
			event.setUpdatedAt(Instant.now());

			outboxEventMapper.insert(event);
			events.add(event);
		}

		// When - Run dispatcher
		dispatcher.dispatch();

		// Then - Verify all events sent
		for (OutboxEvent event : events) {
			OutboxEvent updated = outboxEventMapper.selectById(event.getId());
			assertThat(updated.getStatus()).isEqualTo(OutboxStatus.SENT);
		}

		// And - Verify all messages in Kafka
		String topic = "domain.order";
		List<DomainEventEnvelope> envelopes = consumeAndFindEvents(topic, expectedEventIds);

		assertThat(envelopes).hasSize(3);
		Set<String> actualEventIds = envelopes.stream().map(DomainEventEnvelope::eventId).collect(java.util.stream.Collectors.toSet());
		assertThat(actualEventIds).containsExactlyInAnyOrderElementsOf(expectedEventIds);
	}

	@Test
	@DisplayName("I-01-003: Events sent to correct topic based on domain")
	void events_sent_to_correct_topic_based_on_domain() throws Exception {
		// Given - Insert events for different domains with unique IDs
		String orderEventId = "evt-" + testCounter + "-order-1";
		String vaultEventId = "evt-" + testCounter + "-vault-1";

		OutboxEvent orderEvent = createEvent(orderEventId, "order", "OrderCreated");
		OutboxEvent vaultEvent = createEvent(vaultEventId, "vault", "VaultCreated");

		outboxEventMapper.insert(orderEvent);
		outboxEventMapper.insert(vaultEvent);

		// When - Run dispatcher
		dispatcher.dispatch();

		// Then - Verify order event in domain.order
		DomainEventEnvelope orderEnvelope = consumeAndFindEvent("domain.order", orderEventId);
		assertThat(orderEnvelope.eventId()).isEqualTo(orderEventId);
		assertThat(orderEnvelope.domain()).isEqualTo("order");

		// And - Verify vault event in domain.vault
		DomainEventEnvelope vaultEnvelope = consumeAndFindEvent("domain.vault", vaultEventId);
		assertThat(vaultEnvelope.eventId()).isEqualTo(vaultEventId);
		assertThat(vaultEnvelope.domain()).isEqualTo("vault");
	}

	// Helper method to create test event
	private OutboxEvent createEvent(String eventId, String domain, String eventType) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(eventId);
		event.setDomain(domain);
		event.setAggregateType(domain.substring(0, 1).toUpperCase() + domain.substring(1));
		event.setAggregateId(domain + "-123");
		event.setEventType(eventType);
		event.setPayloadJson("{\"test\":\"data\"}");
		event.setPartitionKey(domain + "-123");
		event.setStatus(OutboxStatus.PENDING);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());
		return event;
	}

	// Helper method to create Kafka consumer
	private KafkaConsumer<String, String> createKafkaConsumer(String topic) {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		config.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config);
		consumer.subscribe(Collections.singletonList(topic));

		return consumer;
	}

	// Helper method to consume messages and find specific event by eventId
	private DomainEventEnvelope consumeAndFindEvent(String topic, String eventId) throws Exception {
		try (KafkaConsumer<String, String> consumer = createKafkaConsumer(topic)) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

			for (ConsumerRecord<String, String> record : records) {
				DomainEventEnvelope envelope = objectMapper.readValue(record.value(), DomainEventEnvelope.class);
				if (envelope.eventId().equals(eventId)) {
					return envelope;
				}
			}

			throw new AssertionError("Event with eventId=" + eventId + " not found in topic " + topic);
		}
	}

	// Helper method to consume messages and find multiple events by eventIds
	private List<DomainEventEnvelope> consumeAndFindEvents(String topic, Set<String> eventIds) throws Exception {
		List<DomainEventEnvelope> found = new ArrayList<>();

		try (KafkaConsumer<String, String> consumer = createKafkaConsumer(topic)) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

			for (ConsumerRecord<String, String> record : records) {
				DomainEventEnvelope envelope = objectMapper.readValue(record.value(), DomainEventEnvelope.class);
				if (eventIds.contains(envelope.eventId())) {
					found.add(envelope);
				}
			}
		}

		return found;
	}

}
