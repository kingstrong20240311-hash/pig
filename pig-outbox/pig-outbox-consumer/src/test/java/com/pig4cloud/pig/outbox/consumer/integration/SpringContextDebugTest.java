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

import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import com.pig4cloud.pig.outbox.consumer.config.OutboxConsumerProperties;
import com.pig4cloud.pig.outbox.consumer.listener.DomainEventKafkaListener;
import com.pig4cloud.pig.outbox.consumer.listener.DomainEventRouter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Context Debug Test - 验证所有必要的 beans 是否被正确加载
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@SpringBootTest
@Testcontainers
class SpringContextDebugTest {

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
	private ApplicationContext applicationContext;

	@Test
	void verifyAllBeansAreLoaded() {
		System.out.println("=== All Bean Names ===");
		String[] beanNames = applicationContext.getBeanDefinitionNames();
		Arrays.stream(beanNames).filter(name -> name.contains("outbox") || name.contains("domain") || name.contains("event")
				|| name.contains("kafka") || name.contains("handler")).sorted().forEach(System.out::println);

		System.out.println("\n=== Checking Critical Beans ===");

		// Check EventHandlerRegistry
		assertThat(applicationContext.containsBean("eventHandlerRegistry"))
			.as("EventHandlerRegistry bean should exist")
			.isTrue();
		EventHandlerRegistry registry = applicationContext.getBean(EventHandlerRegistry.class);
		System.out.println("✓ EventHandlerRegistry found");
		System.out.println("  Registered handlers: " + registry.getAllHandlers().size());

		// Check DomainEventRouter
		assertThat(applicationContext.containsBean("domainEventRouter")).as("DomainEventRouter bean should exist")
			.isTrue();
		DomainEventRouter router = applicationContext.getBean(DomainEventRouter.class);
		System.out.println("✓ DomainEventRouter found");

		// Check DomainEventKafkaListener
		assertThat(applicationContext.containsBean("domainEventKafkaListener"))
			.as("DomainEventKafkaListener bean should exist")
			.isTrue();
		DomainEventKafkaListener listener = applicationContext.getBean(DomainEventKafkaListener.class);
		System.out.println("✓ DomainEventKafkaListener found");

		// Check OutboxConsumerProperties
		assertThat(applicationContext.containsBean("pig.outbox.consumer-com.pig4cloud.pig.outbox.consumer.config.OutboxConsumerProperties"))
			.as("OutboxConsumerProperties bean should exist")
			.isTrue();
		OutboxConsumerProperties properties = applicationContext.getBean(OutboxConsumerProperties.class);
		System.out.println("✓ OutboxConsumerProperties found");
		System.out.println("  Enabled: " + properties.isEnabled());
		System.out.println("  Domains: " + properties.getDomains());
		System.out.println("  Bootstrap servers: " + properties.getBootstrapServers());

		System.out.println("\n=== All Critical Beans Verified ===");
	}

}
