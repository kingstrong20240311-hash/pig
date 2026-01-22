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

package com.pig4cloud.pig.outbox.consumer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.config.EventHandlerAutoConfiguration;
import com.pig4cloud.pig.outbox.api.config.EventHandlerScannerConfiguration;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.consumer.config.OutboxConsumerAutoConfiguration;
import com.pig4cloud.pig.outbox.consumer.listener.DomainEventKafkaListener;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Consumer 事件分发集成测试 I-02: Consumer dispatch - 生产 DomainEventEnvelope 到 Kafka topic -
 * 断言 @DomainEventHandler 方法运行并产生副作用
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@SpringBootTest
@Testcontainers
@DisplayName("I-02: Consumer Dispatch Integration")
@Import({ EventHandlerAutoConfiguration.class, EventHandlerScannerConfiguration.class, OutboxConsumerAutoConfiguration.class })
class ConsumerDispatchIntegrationTest {

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		// Kafka properties
		registry.add("pig.outbox.consumer.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("pig.outbox.consumer.enabled", () -> "true");
		registry.add("pig.outbox.consumer.domains", () -> "order,vault");
		registry.add("pig.outbox.consumer.group-id-prefix", () -> "test-consumer");
	}

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		// Clear test handler invocations
		TestOrderEventHandler.invocations.clear();
		TestVaultEventHandler.invocations.clear();
		
		// 确保 topics 存在
		ensureTopicsExist("domain.order", "domain.vault");
		
		// 等待 Kafka 监听器发现并订阅 topics
		TimeUnit.SECONDS.sleep(3);
	}

	@Test
	@DisplayName("I-02-001: Consumer receives and dispatches event to handler")
	void consumer_receives_and_dispatches_event_to_handler() throws Exception {
		// Wait for Kafka consumer to start
		TimeUnit.SECONDS.sleep(2);

		// Given - Create DomainEventEnvelope
		Map<String, String> headers = Map.of("userId", "user-1", "traceId", "trace-123");
		DomainEventEnvelope envelope = new DomainEventEnvelope("evt-001", "order", "Order", "order-123",
				"OrderCreated", Instant.now(), headers, "{\"amount\":100}");

		// When - Produce message to Kafka
		String topic = "domain.order";
		String messageValue = objectMapper.writeValueAsString(envelope);
		produceMessage(topic, "order-123", messageValue);

		// Then - Wait for handler to be invoked
		await().atMost(15, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			assertThat(TestOrderEventHandler.invocations).as("Handler should be invoked").hasSize(1);
		});

		// Verify handler received correct envelope
		DomainEventEnvelope received = TestOrderEventHandler.invocations.get(0);
		assertThat(received.eventId()).isEqualTo("evt-001");
		assertThat(received.domain()).isEqualTo("order");
		assertThat(received.eventType()).isEqualTo("OrderCreated");
		assertThat(received.aggregateId()).isEqualTo("order-123");
		assertThat(received.headers()).containsEntry("userId", "user-1");
	}

	@Test
	@DisplayName("I-02-002: Consumer routes different event types to correct handlers")
	void consumer_routes_different_event_types_to_correct_handlers() throws Exception {
		// Given - Create OrderCreated event
		DomainEventEnvelope orderEvent = new DomainEventEnvelope("evt-order-1", "order", "Order", "order-123",
				"OrderCreated", Instant.now(), null, "{\"amount\":100}");

		// And - Create VaultCreated event
		DomainEventEnvelope vaultEvent = new DomainEventEnvelope("evt-vault-1", "vault", "Vault", "vault-123",
				"VaultCreated", Instant.now(), null, "{\"balance\":1000}");

		// When - Produce both messages
		produceMessage("domain.order", "order-123", objectMapper.writeValueAsString(orderEvent));
		produceMessage("domain.vault", "vault-123", objectMapper.writeValueAsString(vaultEvent));

		// Then - Wait for handlers to be invoked
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			assertThat(TestOrderEventHandler.invocations).hasSize(1);
			assertThat(TestVaultEventHandler.invocations).hasSize(1);
		});

		// Verify correct routing
		assertThat(TestOrderEventHandler.invocations.get(0).eventType()).isEqualTo("OrderCreated");
		assertThat(TestVaultEventHandler.invocations.get(0).eventType()).isEqualTo("VaultCreated");
	}

	@Autowired
	ApplicationContext applicationContext;
	@Autowired
	EventHandlerRegistry registry;

	@Autowired ApplicationContext ctx;
	@Autowired
	KafkaListenerEndpointRegistry kafkaRegistry;

	@Test
	@DisplayName("I-02-003: Consumer handles multiple events for same domain")
	void consumer_handles_multiple_events_for_same_domain() throws Exception {
		assertThat(applicationContext.getBeansOfType(TestOrderEventHandler.class)).isNotEmpty();
		assertThat(registry.getHandlers("order", "OrderCreated")).isNotEmpty();
		assertThat(ctx.getBeansOfType(DomainEventKafkaListener.class)).isNotEmpty();
		assertThat(kafkaRegistry.getListenerContainers()).isNotEmpty();

		// Given - Create multiple order events
		List<DomainEventEnvelope> events = new ArrayList<>();
		for (int i = 1; i <= 3; i++) {
			DomainEventEnvelope envelope = new DomainEventEnvelope("evt-00" + i, "order", "Order", "order-" + i,
					"OrderCreated", Instant.now(), null, "{\"orderId\":" + i + "}");
			events.add(envelope);
		}

		// When - Produce all messages
		for (DomainEventEnvelope event : events) {
			produceMessage("domain.order", event.aggregateId(), objectMapper.writeValueAsString(event));
		}

		// Then - Wait for all handlers to be invoked
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			assertThat(TestOrderEventHandler.invocations).hasSize(3);
		});

		// Verify all events received
		List<String> eventIds = TestOrderEventHandler.invocations.stream().map(DomainEventEnvelope::eventId).toList();
		assertThat(eventIds).containsExactlyInAnyOrder("evt-001", "evt-002", "evt-003");
	}

	@Test
	@DisplayName("I-02-004: Consumer preserves event ordering per partition key")
	void consumer_preserves_event_ordering_per_partition_key() throws Exception {
		// Given - Create multiple events for same aggregate (same partition key)
		String aggregateId = "order-same";
		List<DomainEventEnvelope> events = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			DomainEventEnvelope envelope = new DomainEventEnvelope("evt-seq-" + i, "order", "Order", aggregateId,
					"OrderUpdated", Instant.now(), null, "{\"sequence\":" + i + "}");
			events.add(envelope);
		}

		// When - Produce all messages with same key (should go to same partition)
		for (DomainEventEnvelope event : events) {
			produceMessage("domain.order", aggregateId, objectMapper.writeValueAsString(event));
		}

		// Then - Wait for all handlers to be invoked
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			assertThat(TestOrderEventHandler.invocations).hasSize(5);
		});

		// Verify ordering (should be in sequence)
		List<String> eventIds = TestOrderEventHandler.invocations.stream().map(DomainEventEnvelope::eventId).toList();
		assertThat(eventIds).containsExactly("evt-seq-1", "evt-seq-2", "evt-seq-3", "evt-seq-4", "evt-seq-5");
	}

	// Helper method to produce message to Kafka
	private void produceMessage(String topic, String key, String value) throws Exception {
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		try (KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {
			ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
			producer.send(record).get(); // Synchronous send
		}
	}

	/**
	 * 确保指定的 topics 存在，如果不存在则创建
	 */
	private void ensureTopicsExist(String... topicNames) throws Exception {
		Map<String, Object> adminConfig = new HashMap<>();
		adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		
		try (AdminClient adminClient = AdminClient.create(adminConfig)) {
			Set<String> existingTopics = adminClient.listTopics().names().get();
			
			List<NewTopic> topicsToCreate = Arrays.stream(topicNames)
				.filter(name -> !existingTopics.contains(name))
				.map(name -> new NewTopic(name, 1, (short) 1))
				.collect(Collectors.toList());
			
			if (!topicsToCreate.isEmpty()) {
				adminClient.createTopics(topicsToCreate).all().get();
				System.out.println("Created topics: " + topicsToCreate.stream()
					.map(NewTopic::name).collect(Collectors.toList()));
			}
		}
	}

	// Test event handlers
	static class TestOrderEventHandler {

		static List<DomainEventEnvelope> invocations = new ArrayList<>();

		@DomainEventHandler(domain = "order", eventType = "OrderCreated")
		public void handleOrderCreated(DomainEventEnvelope event) {
			invocations.add(event);
		}

		@DomainEventHandler(domain = "order", eventType = "OrderUpdated")
		public void handleOrderUpdated(DomainEventEnvelope event) {
			invocations.add(event);
		}

	}

	static class TestVaultEventHandler {

		static List<DomainEventEnvelope> invocations = new ArrayList<>();

		@DomainEventHandler(domain = "vault", eventType = "VaultCreated")
		public void handleVaultCreated(DomainEventEnvelope event) {
			invocations.add(event);
		}

	}

	// Test configuration
	@Configuration
	static class TestConfig {

		@Bean
		public ObjectMapper objectMapper() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			return mapper;
		}

		@Bean
		public TestOrderEventHandler testOrderEventHandler() {
			return new TestOrderEventHandler();
		}

		@Bean
		public TestVaultEventHandler testVaultEventHandler() {
			return new TestVaultEventHandler();
		}

	}

}
