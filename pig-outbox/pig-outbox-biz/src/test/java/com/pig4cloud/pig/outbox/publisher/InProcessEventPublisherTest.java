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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCreatedPayload;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * InProcessEventPublisher 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InProcessEventPublisher 测试")
class InProcessEventPublisherTest {

	@Mock
	private OutboxEventService outboxEventService;

	private ObjectMapper objectMapper;

	private InProcessEventPublisher publisher;

	private DomainEventEnvelope<OrderCreatedPayload> testEvent;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		publisher = new InProcessEventPublisher(outboxEventService, objectMapper);
		testEvent = new DomainEventEnvelope<>("evt-001", "order", "Order", "order-123", "OrderMatched",
				System.currentTimeMillis(), Map.of("userId", "user-1"),
				new OrderCreatedPayload(1001L, 2001L, 3001L, "YES", "OPEN"));
	}

	@Test
	@DisplayName("INP-001: publish saves event to database")
	void publish_saves_event_to_database() {
		// When
		publisher.publish(testEvent);

		// Then
		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventService).save(captor.capture());

		OutboxEvent saved = captor.getValue();
		assertThat(saved.getEventId()).isEqualTo("evt-001");
		assertThat(saved.getDomain()).isEqualTo("order");
		assertThat(saved.getAggregateType()).isEqualTo("Order");
		assertThat(saved.getAggregateId()).isEqualTo("order-123");
		assertThat(saved.getEventType()).isEqualTo("OrderMatched");
		assertThat(saved.getPayloadJson()).contains("orderId");
		assertThat(saved.getPayloadJson()).contains("1001");
		assertThat(saved.getPartitionKey()).isEqualTo("order-123");
		assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(saved.getAttempts()).isEqualTo(0);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("INP-002: publish serializes headers to JSON")
	void publish_serializes_headers() {
		// When
		publisher.publish(testEvent);

		// Then
		ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventService).save(captor.capture());

		OutboxEvent saved = captor.getValue();
		assertThat(saved.getHeadersJson()).isNotNull();
		assertThat(saved.getHeadersJson()).contains("userId");
		assertThat(saved.getHeadersJson()).contains("user-1");
	}

	@Test
	@DisplayName("INP-003: publish handles null headers")
	void publish_handles_null_headers() {
		// Given
		DomainEventEnvelope<OrderCreatedPayload> eventWithoutHeaders = new DomainEventEnvelope<>("evt-002", "order",
				"Order", "order-123", "OrderMatched", System.currentTimeMillis(), null,
				new OrderCreatedPayload(1002L, 2002L, 3002L, "YES", "OPEN"));

		// When
		publisher.publish(eventWithoutHeaders);

		// Then
		verify(outboxEventService).save(any(OutboxEvent.class));
	}

}
