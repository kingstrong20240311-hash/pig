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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DbOutboxEventPublisher 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DbOutboxEventPublisher 测试")
class DbOutboxEventPublisherTest {

	@Mock
	private OutboxEventService outboxEventService;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private DbOutboxEventPublisher publisher;

	private DomainEventEnvelope testEvent;

	@BeforeEach
	void setUp() {
		testEvent = new DomainEventEnvelope("evt-001", "order", "Order", "order-123", "OrderMatched",
				System.currentTimeMillis(), Map.of("userId", "user-1"), "{\"amount\":100}");
	}

	@Test
	@DisplayName("PUB-001: publish 正确映射字段")
	void publish_maps_fields() {
		// Given
		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

		// When
		publisher.publish(testEvent);

		// Then
		verify(outboxEventService).save(captor.capture());
		OutboxEvent saved = captor.getValue();

		assertThat(saved.getEventId()).isEqualTo("evt-001");
		assertThat(saved.getDomain()).isEqualTo("order");
		assertThat(saved.getAggregateType()).isEqualTo("Order");
		assertThat(saved.getAggregateId()).isEqualTo("order-123");
		assertThat(saved.getEventType()).isEqualTo("OrderMatched");
		assertThat(saved.getPayloadJson()).isEqualTo("{\"amount\":100}");
		assertThat(saved.getPartitionKey()).isEqualTo("order-123"); // 默认使用 aggregateId
	}

	@Test
	@DisplayName("PUB-002: publish 设置默认值")
	void publish_sets_defaults() {
		// Given
		DomainEventEnvelope eventWithoutHeaders = new DomainEventEnvelope("evt-002", "order", "Order", "order-456",
				"OrderCreated", System.currentTimeMillis(), null, "{}");

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

		// When
		publisher.publish(eventWithoutHeaders);

		// Then
		verify(outboxEventService).save(captor.capture());
		OutboxEvent saved = captor.getValue();

		assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(saved.getAttempts()).isEqualTo(0);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("PUB-003: publish 序列化 headers")
	void publish_serializes_headers() throws Exception {
		// Given
		String headersJson = "{\"userId\":\"user-1\"}";
		when(objectMapper.writeValueAsString(any())).thenReturn(headersJson);

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

		// When
		publisher.publish(testEvent);

		// Then
		verify(objectMapper).writeValueAsString(testEvent.headers());
		verify(outboxEventService).save(captor.capture());

		OutboxEvent saved = captor.getValue();
		assertThat(saved.getHeadersJson()).isEqualTo(headersJson);
	}

	@Test
	@DisplayName("PUB-004: publish 吞噬 header 序列化异常")
	void publish_swallow_header_serialize_error() throws Exception {
		// Given
		when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("test error") {
		});

		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

		// When
		publisher.publish(testEvent);

		// Then
		verify(outboxEventService).save(captor.capture());
		OutboxEvent saved = captor.getValue();
		assertThat(saved.getHeadersJson()).isNull();
	}

	@Test
	@DisplayName("PUB-005: publish 在 save 失败时回滚事务")
	void publish_transaction_rollback_on_save_error() {
		// Given
		doThrow(new RuntimeException("DB error")).when(outboxEventService).save(any());

		// When & Then
		assertThatThrownBy(() -> publisher.publish(testEvent)).isInstanceOf(RuntimeException.class)
			.hasMessage("DB error");

		verify(outboxEventService).save(any());
	}

}
