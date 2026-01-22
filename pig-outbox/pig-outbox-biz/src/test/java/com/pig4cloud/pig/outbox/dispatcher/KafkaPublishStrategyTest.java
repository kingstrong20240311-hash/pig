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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.config.OutboxKafkaProperties;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * KafkaPublishStrategy 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaPublishStrategy 测试")
class KafkaPublishStrategyTest {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private ObjectMapper objectMapper;

	private OutboxKafkaProperties kafkaProperties;

	private KafkaPublishStrategy strategy;

	@Captor
	private ArgumentCaptor<String> topicCaptor;

	@Captor
	private ArgumentCaptor<String> keyCaptor;

	@Captor
	private ArgumentCaptor<String> valueCaptor;

	private OutboxEvent testEvent;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		kafkaProperties = new OutboxKafkaProperties();
		kafkaProperties.setSendTimeoutSeconds(30);
		strategy = new KafkaPublishStrategy(kafkaTemplate, objectMapper, kafkaProperties);

		testEvent = new OutboxEvent();
		testEvent.setId(1L);
		testEvent.setEventId("evt-001");
		testEvent.setDomain("order");
		testEvent.setAggregateType("Order");
		testEvent.setAggregateId("order-123");
		testEvent.setEventType("OrderCreated");
		testEvent.setPayloadJson("{\"amount\":100}");
		testEvent.setHeadersJson("{\"userId\":\"user-1\"}");
		testEvent.setPartitionKey("order-123");
		testEvent.setStatus(OutboxStatus.SENDING);
		testEvent.setAttempts(0);
		testEvent.setCreatedAt(Instant.now());
		testEvent.setUpdatedAt(Instant.now());
	}

	@Test
	@DisplayName("KPS-001: publish success - sends to correct topic")
	void publish_success_sends_to_correct_topic() throws Exception {
		// Given
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When
		strategy.publish(testEvent);

		// Then
		verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());
		assertThat(topicCaptor.getValue()).isEqualTo("domain.order");
	}

	@Test
	@DisplayName("KPS-002: publish success - uses partition key as Kafka key")
	void publish_success_uses_partition_key_as_kafka_key() throws Exception {
		// Given
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When
		strategy.publish(testEvent);

		// Then
		verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), anyString());
		assertThat(keyCaptor.getValue()).isEqualTo("order-123");
	}

	@Test
	@DisplayName("KPS-003: publish success - serializes DomainEventEnvelope correctly")
	void publish_success_serializes_envelope_correctly() throws Exception {
		// Given
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When
		strategy.publish(testEvent);

		// Then
		verify(kafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());
		String messageValue = valueCaptor.getValue();

		DomainEventEnvelope envelope = objectMapper.readValue(messageValue, DomainEventEnvelope.class);
		assertThat(envelope.eventId()).isEqualTo("evt-001");
		assertThat(envelope.domain()).isEqualTo("order");
		assertThat(envelope.aggregateType()).isEqualTo("Order");
		assertThat(envelope.aggregateId()).isEqualTo("order-123");
		assertThat(envelope.eventType()).isEqualTo("OrderCreated");
		assertThat(envelope.payloadJson()).isEqualTo("{\"amount\":100}");
		assertThat(envelope.headers()).containsEntry("userId", "user-1");
	}

	@Test
	@DisplayName("KPS-004: publish success - handles event without headers")
	void publish_success_handles_event_without_headers() throws Exception {
		// Given
		testEvent.setHeadersJson(null);
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When
		strategy.publish(testEvent);

		// Then
		verify(kafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());
		String messageValue = valueCaptor.getValue();

		DomainEventEnvelope envelope = objectMapper.readValue(messageValue, DomainEventEnvelope.class);
		assertThat(envelope.headers()).isNull();
	}

	@Test
	@DisplayName("KPS-005: publish success - handles event with empty headers")
	void publish_success_handles_event_with_empty_headers() throws Exception {
		// Given
		testEvent.setHeadersJson("");
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When
		strategy.publish(testEvent);

		// Then
		verify(kafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());
		String messageValue = valueCaptor.getValue();

		DomainEventEnvelope envelope = objectMapper.readValue(messageValue, DomainEventEnvelope.class);
		assertThat(envelope.headers()).isNull();
	}

	@Test
	@DisplayName("KPS-006: publish failure - throws RuntimeException on Kafka failure")
	void publish_failure_throws_runtime_exception() {
		// Given
		CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
		future.completeExceptionally(new RuntimeException("Kafka unavailable"));
		lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When & Then
		assertThatThrownBy(() -> strategy.publish(testEvent)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Kafka publish failed");
	}

	@Test
	@DisplayName("KPS-007: publish failure - throws RuntimeException on timeout")
	void publish_failure_throws_runtime_exception_on_timeout() {
		// Given
		kafkaProperties.setSendTimeoutSeconds(1);
		strategy = new KafkaPublishStrategy(kafkaTemplate, objectMapper, kafkaProperties);

		CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
		// Never complete the future to simulate timeout
		lenient().when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When & Then
		assertThatThrownBy(() -> strategy.publish(testEvent)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Kafka publish failed");
	}

	@Test
	@DisplayName("KPS-008: publish success - handles different domains")
	void publish_success_handles_different_domains() throws Exception {
		// Given
		testEvent.setDomain("vault");
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When
		strategy.publish(testEvent);

		// Then
		verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), anyString());
		assertThat(topicCaptor.getValue()).isEqualTo("domain.vault");
	}

	@Test
	@DisplayName("KPS-009: publish success - handles malformed headers gracefully")
	void publish_success_handles_malformed_headers_gracefully() throws Exception {
		// Given
		testEvent.setHeadersJson("{invalid json");
		CompletableFuture<SendResult<String, String>> future = createSuccessfulSendResult();
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

		// When & Then - should not throw, just log warning
		assertThatCode(() -> strategy.publish(testEvent)).doesNotThrowAnyException();

		verify(kafkaTemplate).send(anyString(), anyString(), valueCaptor.capture());
		String messageValue = valueCaptor.getValue();

		DomainEventEnvelope envelope = objectMapper.readValue(messageValue, DomainEventEnvelope.class);
		assertThat(envelope.headers()).isNull(); // Headers should be null due to parsing
													// failure
	}

	// Helper method to create successful SendResult
	private CompletableFuture<SendResult<String, String>> createSuccessfulSendResult() {
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>("domain.order", "order-123",
				"{\"test\":\"message\"}");
		RecordMetadata metadata = new RecordMetadata(new TopicPartition("domain.order", 0), 0L, 0, 0L, 0, 0);
		SendResult<String, String> sendResult = new SendResult<>(producerRecord, metadata);

		CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
		future.complete(sendResult);
		return future;
	}

}
