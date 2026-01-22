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
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * InProcessPublishStrategy 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InProcessPublishStrategy 测试")
class InProcessPublishStrategyTest {

	@Mock
	private EventHandlerRegistry registry;

	private ObjectMapper objectMapper;

	private InProcessPublishStrategy strategy;

	private OutboxEvent testEvent;

	private TestHandler handler1;

	private TestHandler handler2;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		strategy = new InProcessPublishStrategy(registry, objectMapper);

		testEvent = new OutboxEvent();
		testEvent.setId(1L);
		testEvent.setEventId("evt-001");
		testEvent.setDomain("order");
		testEvent.setAggregateType("Order");
		testEvent.setAggregateId("order-123");
		testEvent.setEventType("OrderMatched");
		testEvent.setPayloadJson("{\"amount\":100}");
		testEvent.setHeadersJson("{\"userId\":\"user-1\"}");
		testEvent.setPartitionKey("order-123");
		testEvent.setStatus(OutboxStatus.SENDING);
		testEvent.setAttempts(0);
		testEvent.setCreatedAt(Instant.now());
		testEvent.setUpdatedAt(Instant.now());

		handler1 = new TestHandler();
		handler2 = new TestHandler();
	}

	@Test
	@DisplayName("IPS-001: publish invokes handler")
	void publish_invokes_handlers() throws Exception {
		// Given
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<EventHandlerRegistry.HandlerMethod> handlers = List
			.of(new EventHandlerRegistry.HandlerMethod(handler1, method, "group-1"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		strategy.publish(testEvent);

		// Then
		assertThat(handler1.invocations).hasSize(1);
		DomainEventEnvelope received = handler1.invocations.get(0);
		assertThat(received.eventId()).isEqualTo("evt-001");
		assertThat(received.domain()).isEqualTo("order");
		assertThat(received.aggregateId()).isEqualTo("order-123");
		assertThat(received.payloadJson()).isEqualTo("{\"amount\":100}");
	}

	@Test
	@DisplayName("IPS-002: publish invokes multiple handlers")
	void publish_invokes_multiple_handlers() throws Exception {
		// Given
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<EventHandlerRegistry.HandlerMethod> handlers = List.of(
				new EventHandlerRegistry.HandlerMethod(handler1, method, "group-1"),
				new EventHandlerRegistry.HandlerMethod(handler2, method, "group-2"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		strategy.publish(testEvent);

		// Then
		assertThat(handler1.invocations).hasSize(1);
		assertThat(handler2.invocations).hasSize(1);
	}

	@Test
	@DisplayName("IPS-003: publish no handlers warn and return")
	void publish_no_handlers_warn_and_return() {
		// Given
		when(registry.getHandlers(anyString(), anyString())).thenReturn(List.of());

		// When & Then
		assertThatCode(() -> strategy.publish(testEvent)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("IPS-004: publish handler exception throws")
	void publish_handler_exception_throws() throws Exception {
		// Given
		Method throwMethod = ThrowingHandler.class.getMethod("handleWithException", DomainEventEnvelope.class);

		ThrowingHandler throwingHandler = new ThrowingHandler();

		List<EventHandlerRegistry.HandlerMethod> handlers = List
			.of(new EventHandlerRegistry.HandlerMethod(throwingHandler, throwMethod, "group-1"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When & Then
		assertThatThrownBy(() -> strategy.publish(testEvent)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to handle event in-process");
	}

	@Test
	@DisplayName("IPS-005: publish calls handlers in order")
	void publish_calls_handlers_in_order() throws Exception {
		// Given
		OrderTrackingHandler handlerA = new OrderTrackingHandler("A");
		OrderTrackingHandler handlerB = new OrderTrackingHandler("B");

		Method method = OrderTrackingHandler.class.getMethod("handle", DomainEventEnvelope.class);

		List<EventHandlerRegistry.HandlerMethod> handlers = List.of(
				new EventHandlerRegistry.HandlerMethod(handlerA, method, "group-a"),
				new EventHandlerRegistry.HandlerMethod(handlerB, method, "group-b"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		strategy.publish(testEvent);

		// Then
		assertThat(OrderTrackingHandler.callOrder).containsExactly("A", "B");
	}

	@Test
	@DisplayName("IPS-006: publish deserializes headers")
	void publish_deserializes_headers() throws Exception {
		// Given
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<EventHandlerRegistry.HandlerMethod> handlers = List
			.of(new EventHandlerRegistry.HandlerMethod(handler1, method, "group-1"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		strategy.publish(testEvent);

		// Then
		DomainEventEnvelope received = handler1.invocations.get(0);
		assertThat(received.headers()).isNotNull();
		assertThat(received.headers()).containsEntry("userId", "user-1");
	}

	// Test helper classes
	static class TestHandler {

		List<DomainEventEnvelope> invocations = new ArrayList<>();

		public void handle(DomainEventEnvelope event) {
			invocations.add(event);
		}

	}

	static class ThrowingHandler {

		boolean called = false;

		public void handleWithException(DomainEventEnvelope event) {
			called = true;
			throw new RuntimeException("Handler error");
		}

	}

	static class OrderTrackingHandler {

		static List<String> callOrder = new ArrayList<>();

		private final String name;

		OrderTrackingHandler(String name) {
			this.name = name;
			callOrder.clear();
		}

		public void handle(DomainEventEnvelope event) {
			callOrder.add(name);
		}

	}

}
