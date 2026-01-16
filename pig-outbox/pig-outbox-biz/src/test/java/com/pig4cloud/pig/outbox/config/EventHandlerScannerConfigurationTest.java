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

package com.pig4cloud.pig.outbox.config;

import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.handler.EventHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EventHandlerScannerConfiguration 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@DisplayName("EventHandlerScannerConfiguration 测试")
class EventHandlerScannerConfigurationTest {

	private EventHandlerRegistry registry;

	private EventHandlerScannerConfiguration scanner;

	@BeforeEach
	void setUp() {
		registry = new EventHandlerRegistry();
		scanner = new EventHandlerScannerConfiguration(registry);
	}

	@Test
	@DisplayName("SCN-001: scan 注册带注解的方法")
	void scan_registers_annotated_methods() {
		// Given
		BeanWithOneAnnotatedMethod bean = new BeanWithOneAnnotatedMethod();

		// When
		scanner.postProcessAfterInitialization(bean, "testBean");

		// Then
		List<EventHandlerRegistry.HandlerMethod> handlers = registry.getHandlers("order", "OrderCreated");
		assertThat(handlers).hasSize(1);
		assertThat(handlers.get(0).getBean()).isSameAs(bean);
		assertThat(handlers.get(0).getMethod().getName()).isEqualTo("handleOrderCreated");
		assertThat(handlers.get(0).getGroupId()).isEqualTo("settlement-service");
	}

	@Test
	@DisplayName("SCN-002: scan 跳过非注解方法")
	void scan_skips_non_annotated_methods() {
		// Given
		BeanWithNoAnnotation bean = new BeanWithNoAnnotation();

		// When
		scanner.postProcessAfterInitialization(bean, "testBean");

		// Then
		assertThat(registry.getAllHandlers()).isEmpty();
	}

	@Test
	@DisplayName("SCN-003: scan 同一 bean 多个注解方法")
	void scan_multiple_methods_in_one_bean() {
		// Given
		BeanWithMultipleAnnotatedMethods bean = new BeanWithMultipleAnnotatedMethods();

		// When
		scanner.postProcessAfterInitialization(bean, "testBean");

		// Then
		List<EventHandlerRegistry.HandlerMethod> handlers1 = registry.getHandlers("order", "OrderCreated");
		assertThat(handlers1).hasSize(1);

		List<EventHandlerRegistry.HandlerMethod> handlers2 = registry.getHandlers("order", "OrderMatched");
		assertThat(handlers2).hasSize(1);
	}

	@Test
	@DisplayName("SCN-004: scan 多个 bean")
	void scan_multiple_beans() {
		// Given
		BeanWithOneAnnotatedMethod bean1 = new BeanWithOneAnnotatedMethod();
		AnotherBeanWithAnnotation bean2 = new AnotherBeanWithAnnotation();

		// When
		scanner.postProcessAfterInitialization(bean1, "bean1");
		scanner.postProcessAfterInitialization(bean2, "bean2");

		// Then
		List<EventHandlerRegistry.HandlerMethod> handlers1 = registry.getHandlers("order", "OrderCreated");
		assertThat(handlers1).hasSize(1);

		List<EventHandlerRegistry.HandlerMethod> handlers2 = registry.getHandlers("vault", "VaultCreated");
		assertThat(handlers2).hasSize(1);
	}

	// 测试辅助类
	static class BeanWithOneAnnotatedMethod {

		@DomainEventHandler(domain = "order", eventType = "OrderCreated", groupId = "settlement-service")
		public void handleOrderCreated(DomainEventEnvelope event) {
		}

	}

	static class BeanWithNoAnnotation {

		public void someMethod() {
		}

	}

	static class BeanWithMultipleAnnotatedMethods {

		@DomainEventHandler(domain = "order", eventType = "OrderCreated", groupId = "group-1")
		public void handleOrderCreated(DomainEventEnvelope event) {
		}

		@DomainEventHandler(domain = "order", eventType = "OrderMatched", groupId = "group-2")
		public void handleOrderMatched(DomainEventEnvelope event) {
		}

	}

	static class AnotherBeanWithAnnotation {

		@DomainEventHandler(domain = "vault", eventType = "VaultCreated", groupId = "group-3")
		public void handleVaultCreated(DomainEventEnvelope event) {
		}

	}

}
