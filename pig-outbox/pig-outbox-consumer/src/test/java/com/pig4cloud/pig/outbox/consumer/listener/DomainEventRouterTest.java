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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry.HandlerMethod;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * DomainEventRouter 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DomainEventRouter 测试")
class DomainEventRouterTest {

	@Mock
	private EventHandlerRegistry registry;

	private ObjectMapper objectMapper;

	private DomainEventRouter router;

	private TestHandler handlerA;

	private TestHandler handlerB;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		router = new DomainEventRouter(registry, objectMapper);
		handlerA = new TestHandler();
		handlerB = new TestHandler();
	}

	@Test
	@DisplayName("DER-001: route invokes single handler")
	void route_invokes_single_handler() throws Exception {
		// Given
		DomainEventEnvelope envelope = createTestEnvelope("order", "OrderCreated");
		String messageValue = objectMapper.writeValueAsString(envelope);

		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<HandlerMethod> handlers = List.of(new HandlerMethod(handlerA, method, "group-1"));

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When
		router.route(messageValue);

		// Then
		assertThat(handlerA.invocations).hasSize(1);
		DomainEventEnvelope received = handlerA.invocations.get(0);
		assertThat(received.eventId()).isEqualTo("evt-001");
		assertThat(received.domain()).isEqualTo("order");
		assertThat(received.eventType()).isEqualTo("OrderCreated");
		assertThat(received.aggregateId()).isEqualTo("order-123");
	}

	@Test
	@DisplayName("DER-002: route invokes multiple handlers for same event")
	void route_invokes_multiple_handlers_for_same_event() throws Exception {
		// Given
		DomainEventEnvelope envelope = createTestEnvelope("order", "OrderCreated");
		String messageValue = objectMapper.writeValueAsString(envelope);

		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<HandlerMethod> handlers = List.of(new HandlerMethod(handlerA, method, "group-1"),
				new HandlerMethod(handlerB, method, "group-2"));

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When
		router.route(messageValue);

		// Then
		assertThat(handlerA.invocations).hasSize(1);
		assertThat(handlerB.invocations).hasSize(1);
	}

	@Test
	@DisplayName("DER-003: route dispatches to correct handler based on eventType")
	void route_dispatches_to_correct_handler_based_on_eventType() throws Exception {
		// Given - two handlers for different eventTypes in same domain
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);

		// Handler A for OrderCreated
		when(registry.getHandlers("order", "OrderCreated"))
			.thenReturn(List.of(new HandlerMethod(handlerA, method, "group-a")));

		// Handler B for OrderMatched (lenient because we won't call it in this test)
		lenient().when(registry.getHandlers("order", "OrderMatched"))
			.thenReturn(List.of(new HandlerMethod(handlerB, method, "group-b")));

		// When - send OrderCreated event
		DomainEventEnvelope envelope = createTestEnvelope("order", "OrderCreated");
		String messageValue = objectMapper.writeValueAsString(envelope);
		router.route(messageValue);

		// Then - only handler A invoked
		assertThat(handlerA.invocations).hasSize(1);
		assertThat(handlerB.invocations).isEmpty();
		assertThat(handlerA.invocations.get(0).eventType()).isEqualTo("OrderCreated");
	}

	@Test
	@DisplayName("DER-004: route handles no registered handlers gracefully")
	void route_handles_no_registered_handlers_gracefully() throws Exception {
		// Given
		DomainEventEnvelope envelope = createTestEnvelope("order", "UnknownEvent");
		String messageValue = objectMapper.writeValueAsString(envelope);

		when(registry.getHandlers(anyString(), anyString())).thenReturn(List.of());

		// When & Then - should not throw, just log warning
		assertThatCode(() -> router.route(messageValue)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("DER-005: route throws RuntimeException on handler failure")
	void route_throws_runtime_exception_on_handler_failure() throws Exception {
		// Given
		DomainEventEnvelope envelope = createTestEnvelope("order", "OrderCreated");
		String messageValue = objectMapper.writeValueAsString(envelope);

		Method throwMethod = ThrowingHandler.class.getMethod("handleWithException", DomainEventEnvelope.class);
		ThrowingHandler throwingHandler = new ThrowingHandler();
		List<HandlerMethod> handlers = List.of(new HandlerMethod(throwingHandler, throwMethod, "group-1"));

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When & Then
		assertThatThrownBy(() -> router.route(messageValue)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Event routing failed");

		assertThat(throwingHandler.called).isTrue();
	}

	@Test
	@DisplayName("DER-006: route throws RuntimeException on invalid JSON")
	void route_throws_runtime_exception_on_invalid_json() {
		// Given
		String invalidJson = "{invalid json";

		// When & Then
		assertThatThrownBy(() -> router.route(invalidJson)).isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Event routing failed");
	}

	@Test
	@DisplayName("DER-007: route calls handlers in order")
	void route_calls_handlers_in_order() throws Exception {
		// Given
		OrderTrackingHandler handlerX = new OrderTrackingHandler("X");
		OrderTrackingHandler handlerY = new OrderTrackingHandler("Y");

		Method method = OrderTrackingHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<HandlerMethod> handlers = List.of(new HandlerMethod(handlerX, method, "group-x"),
				new HandlerMethod(handlerY, method, "group-y"));

		DomainEventEnvelope envelope = createTestEnvelope("order", "OrderCreated");
		String messageValue = objectMapper.writeValueAsString(envelope);

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When
		router.route(messageValue);

		// Then
		assertThat(OrderTrackingHandler.callOrder).containsExactly("X", "Y");
	}

	@Test
	@DisplayName("DER-008: route deserializes envelope with headers")
	void route_deserializes_envelope_with_headers() throws Exception {
		// Given
		Map<String, String> headers = Map.of("userId", "user-1", "traceId", "trace-123");
		DomainEventEnvelope envelope = new DomainEventEnvelope("evt-001", "order", "Order", "order-123", "OrderCreated",
				System.currentTimeMillis(), headers, "{\"amount\":100}");
		String messageValue = objectMapper.writeValueAsString(envelope);

		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<HandlerMethod> handlers = List.of(new HandlerMethod(handlerA, method, "group-1"));

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When
		router.route(messageValue);

		// Then
		assertThat(handlerA.invocations).hasSize(1);
		DomainEventEnvelope received = handlerA.invocations.get(0);
		assertThat(received.headers()).isNotNull();
		assertThat(received.headers()).containsEntry("userId", "user-1");
		assertThat(received.headers()).containsEntry("traceId", "trace-123");
	}

	@Test
	@DisplayName("DER-009: route handles envelope without headers")
	void route_handles_envelope_without_headers() throws Exception {
		// Given
		DomainEventEnvelope envelope = new DomainEventEnvelope("evt-001", "order", "Order", "order-123", "OrderCreated",
				System.currentTimeMillis(), null, "{\"amount\":100}");
		String messageValue = objectMapper.writeValueAsString(envelope);

		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<HandlerMethod> handlers = List.of(new HandlerMethod(handlerA, method, "group-1"));

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When
		router.route(messageValue);

		// Then
		assertThat(handlerA.invocations).hasSize(1);
		DomainEventEnvelope received = handlerA.invocations.get(0);
		assertThat(received.headers()).isNull();
	}

	@Test
	@DisplayName("DER-010: route stops on first handler failure")
	void route_stops_on_first_handler_failure() throws Exception {
		// Given
		DomainEventEnvelope envelope = createTestEnvelope("order", "OrderCreated");
		String messageValue = objectMapper.writeValueAsString(envelope);

		Method throwMethod = ThrowingHandler.class.getMethod("handleWithException", DomainEventEnvelope.class);
		Method normalMethod = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);

		ThrowingHandler throwingHandler = new ThrowingHandler();

		// First handler throws, second handler should not be called
		List<HandlerMethod> handlers = List.of(new HandlerMethod(throwingHandler, throwMethod, "group-1"),
				new HandlerMethod(handlerA, normalMethod, "group-2"));

		when(registry.getHandlers("order", "OrderCreated")).thenReturn(handlers);

		// When & Then
		assertThatThrownBy(() -> router.route(messageValue)).isInstanceOf(RuntimeException.class);

		// First handler was called
		assertThat(throwingHandler.called).isTrue();
		// Second handler was NOT called (because first one threw)
		assertThat(handlerA.invocations).isEmpty();
	}

	// Helper method to create test envelope
	private DomainEventEnvelope createTestEnvelope(String domain, String eventType) {
		return new DomainEventEnvelope("evt-001", domain, "Order", "order-123", eventType, System.currentTimeMillis(), null,
				"{\"amount\":100}");
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
