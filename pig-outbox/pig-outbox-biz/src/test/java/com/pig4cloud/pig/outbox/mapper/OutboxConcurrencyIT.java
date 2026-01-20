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

package com.pig4cloud.pig.outbox.mapper;

import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Outbox 并发测试
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Testcontainers
@SpringBootTest
@DisplayName("Outbox 并发测试")
class OutboxConcurrencyIT {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("test")
		.withInitScript("db/outbox_event.sql");

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@Autowired
	private OutboxEventMapper mapper;

	@Autowired
	private OutboxEventService service;

	@BeforeEach
	void setUp() {
		mapper.delete(null);
	}

	@Test
	@DisplayName("CON-001: claimEvents 竞争")
	void claimEvents_competition() throws InterruptedException {
		// Given
		OutboxEvent evt = createEvent("evt-1", OutboxStatus.PENDING);
		mapper.insert(evt);

		AtomicInteger worker1Claims = new AtomicInteger(0);
		AtomicInteger worker2Claims = new AtomicInteger(0);

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(2);

		// When - 两个线程同时尝试 claim
		Thread t1 = new Thread(() -> {
			try {
				startLatch.await();
				int claimed = mapper.claimEvents(List.of(evt.getId()), "worker-1", Instant.now());
				worker1Claims.set(claimed);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				doneLatch.countDown();
			}
		});

		Thread t2 = new Thread(() -> {
			try {
				startLatch.await();
				int claimed = mapper.claimEvents(List.of(evt.getId()), "worker-2", Instant.now());
				worker2Claims.set(claimed);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				doneLatch.countDown();
			}
		});

		t1.start();
		t2.start();

		startLatch.countDown(); // 同时开始
		doneLatch.await(5, TimeUnit.SECONDS);

		// Then - 仅一方成功
		int totalClaims = worker1Claims.get() + worker2Claims.get();
		assertThat(totalClaims).isEqualTo(1);

		OutboxEvent updated = mapper.selectById(evt.getId());
		assertThat(updated.getStatus()).isEqualTo(OutboxStatus.SENDING);
		assertThat(updated.getLockedBy()).isIn("worker-1", "worker-2");
	}

	@Test
	@DisplayName("CON-002: publish 高并发")
	void publish_high_concurrency() throws InterruptedException {
		// Given
		int threadCount = 10;
		int eventsPerThread = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// When
		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					for (int j = 0; j < eventsPerThread; j++) {
						OutboxEvent event = createEvent("evt-" + threadId + "-" + j, OutboxStatus.PENDING);
						service.save(event);
					}
				}
				finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		executor.shutdown();

		// Then
		long totalEvents = mapper.selectCount(null);
		assertThat(totalEvents).isEqualTo(threadCount * eventsPerThread);

		// 检查无重复 event_id
		List<OutboxEvent> allEvents = mapper.selectList(null);
		long uniqueEventIds = allEvents.stream().map(OutboxEvent::getEventId).distinct().count();
		assertThat(uniqueEventIds).isEqualTo(totalEvents);
	}

	@Test
	@DisplayName("CON-003: 锁超时后重新 claim")
	void reclaim_after_lock_timeout() {
		// Given
		OutboxEvent evt = createEvent("evt-1", OutboxStatus.SENDING);
		evt.setLockedBy("worker-1");
		evt.setLockedAt(Instant.now().minusSeconds(120)); // 已超时（超时设置为60秒）
		mapper.insert(evt);

		// When
		int reclaimed = mapper.claimEvents(List.of(evt.getId()), "worker-2", Instant.now());

		// Then
		assertThat(reclaimed).isEqualTo(1);

		OutboxEvent updated = mapper.selectById(evt.getId());
		assertThat(updated.getLockedBy()).isEqualTo("worker-2");
		assertThat(updated.getLockedAt()).isAfter(evt.getLockedAt());
	}

	// 辅助方法
	private OutboxEvent createEvent(String eventId, OutboxStatus status) {
		OutboxEvent event = new OutboxEvent();
		event.setEventId(eventId);
		event.setDomain("order");
		event.setAggregateType("Order");
		event.setAggregateId("agg-" + eventId);
		event.setEventType("OrderCreated");
		event.setPayloadJson("{}");
		event.setPartitionKey("partition-key");
		event.setStatus(status);
		event.setAttempts(0);
		event.setCreatedAt(Instant.now());
		event.setUpdatedAt(Instant.now());
		return event;
	}

}
