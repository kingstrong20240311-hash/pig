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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Handler Registration Test - 验证事件处理器是否被正确注册
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@SpringBootTest
@Testcontainers
@Import({ EventHandlerAutoConfiguration.class, EventHandlerScannerConfiguration.class })
class HandlerRegistrationTest {

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("pig.outbox.consumer.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("pig.outbox.consumer.enabled", () -> "true");
		registry.add("pig.outbox.consumer.domains", () -> "order,vault");
		registry.add("pig.outbox.consumer.group-id-prefix", () -> "test-consumer");
	}

	@Autowired
	private EventHandlerRegistry eventHandlerRegistry;

	@Test
	void testHandlersAreRegistered() {
		System.out.println("=== Handler Registration Test ===");

		Map<String, List<EventHandlerRegistry.HandlerMethod>> allHandlers = eventHandlerRegistry.getAllHandlers();
		System.out.println("Total handler keys: " + allHandlers.size());

		allHandlers.forEach((key, handlers) -> {
			System.out.println("Key: " + key);
			handlers.forEach(h -> {
				System.out.println(
						"  Handler: " + h.getBean().getClass().getSimpleName() + "." + h.getMethod().getName());
			});
		});

		// Verify OrderCreated handler
		List<EventHandlerRegistry.HandlerMethod> orderCreatedHandlers = eventHandlerRegistry.getHandlers("order",
				"OrderCreated");
		assertThat(orderCreatedHandlers).as("OrderCreated handler should be registered").hasSize(1);

		// Verify VaultCreated handler
		List<EventHandlerRegistry.HandlerMethod> vaultCreatedHandlers = eventHandlerRegistry.getHandlers("vault",
				"VaultCreated");
		assertThat(vaultCreatedHandlers).as("VaultCreated handler should be registered").hasSize(1);

		System.out.println("✓ All handlers registered successfully");
	}

	// Test event handlers
	static class TestOrderEventHandler {

		static List<DomainEventEnvelope> invocations = new ArrayList<>();

		@DomainEventHandler(domain = "order", eventType = "OrderCreated")
		public void handleOrderCreated(DomainEventEnvelope event) {
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
