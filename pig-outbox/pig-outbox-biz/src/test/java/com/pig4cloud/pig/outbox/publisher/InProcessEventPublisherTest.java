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

import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.handler.EventHandlerRegistry;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
	private EventHandlerRegistry registry;

	private InProcessEventPublisher publisher;

	private DomainEventEnvelope testEvent;

	private TestHandler handler1;

	private TestHandler handler2;

	@BeforeEach
	void setUp() {
		publisher = new InProcessEventPublisher(registry);
		testEvent = new DomainEventEnvelope("evt-001", "order", "Order", "order-123", "OrderMatched", Instant.now(),
				Map.of("userId", "user-1"), "{\"amount\":100}");

		handler1 = new TestHandler();
		handler2 = new TestHandler();
	}

	@Test
	@DisplayName("INP-001: publish 调用 handler")
	void publish_invokes_handlers() throws Exception {
		// Given
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<EventHandlerRegistry.HandlerMethod> handlers = List
			.of(new EventHandlerRegistry.HandlerMethod(handler1, method, "group-1"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		publisher.publish(testEvent);

		// Then
		assertThat(handler1.invocations).hasSize(1);
		assertThat(handler1.invocations.get(0)).isSameAs(testEvent);
	}

	@Test
	@DisplayName("INP-002: publish 调用多个 handler")
	void publish_invokes_multiple_handlers() throws Exception {
		// Given
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<EventHandlerRegistry.HandlerMethod> handlers = List.of(
				new EventHandlerRegistry.HandlerMethod(handler1, method, "group-1"),
				new EventHandlerRegistry.HandlerMethod(handler2, method, "group-2"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		publisher.publish(testEvent);

		// Then
		assertThat(handler1.invocations).hasSize(1);
		assertThat(handler2.invocations).hasSize(1);
	}

	@Test
	@DisplayName("INP-003: publish 无 handler 时记录 warn 并返回")
	void publish_no_handlers_warn_and_return() {
		// Given
		when(registry.getHandlers(anyString(), anyString())).thenReturn(List.of());

		// When & Then
		assertThatCode(() -> publisher.publish(testEvent)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("INP-004: publish handler 抛异常时被吞")
	void publish_handler_exception_is_swallowed() throws Exception {
		// Given
		Method throwMethod = ThrowingHandler.class.getMethod("handleWithException", DomainEventEnvelope.class);
		Method normalMethod = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);

		ThrowingHandler throwingHandler = new ThrowingHandler();

		List<EventHandlerRegistry.HandlerMethod> handlers = List.of(
				new EventHandlerRegistry.HandlerMethod(throwingHandler, throwMethod, "group-1"),
				new EventHandlerRegistry.HandlerMethod(handler1, normalMethod, "group-2"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		assertThatCode(() -> publisher.publish(testEvent)).doesNotThrowAnyException();

		// Then
		assertThat(throwingHandler.called).isTrue();
		assertThat(handler1.invocations).hasSize(1); // 第二个 handler 仍被调用
	}

	@Test
	@DisplayName("INP-005: publish 按注册顺序调用 handler")
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
		publisher.publish(testEvent);

		// Then
		assertThat(OrderTrackingHandler.callOrder).containsExactly("A", "B");
	}

	@Test
	@DisplayName("INP-006: publish 传递完整 event")
	void publish_passes_event_as_is() throws Exception {
		// Given
		Method method = TestHandler.class.getMethod("handle", DomainEventEnvelope.class);
		List<EventHandlerRegistry.HandlerMethod> handlers = List
			.of(new EventHandlerRegistry.HandlerMethod(handler1, method, "group-1"));

		when(registry.getHandlers("order", "OrderMatched")).thenReturn(handlers);

		// When
		publisher.publish(testEvent);

		// Then
		DomainEventEnvelope received = handler1.invocations.get(0);
		assertThat(received.eventId()).isEqualTo("evt-001");
		assertThat(received.domain()).isEqualTo("order");
		assertThat(received.aggregateId()).isEqualTo("order-123");
		assertThat(received.headers()).containsEntry("userId", "user-1");
		assertThat(received.payloadJson()).isEqualTo("{\"amount\":100}");
	}

	// 测试辅助类
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
			callOrder.clear(); // 重置调用顺序
		}

		public void handle(DomainEventEnvelope event) {
			callOrder.add(name);
		}

	}

}
