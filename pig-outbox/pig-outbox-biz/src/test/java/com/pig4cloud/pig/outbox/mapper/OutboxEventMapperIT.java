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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * OutboxEventMapper 集成测试
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Testcontainers
@SpringBootTest
@Transactional
@DisplayName("OutboxEventMapper 集成测试")
class OutboxEventMapperIT {

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

	@BeforeEach
	void setUp() {
		// 清理数据
		mapper.delete(null);
	}

	@Test
	@DisplayName("MAP-001: selectPending 仅返回待处理状态")
	void selectPending_only_status_0_3() {
		// Given
		insertEvent("evt-1", OutboxStatus.NEW); // 0 - 应返回
		insertEvent("evt-2", OutboxStatus.RETRY); // 3 - 应返回

		// SENDING 但锁未超时 - 不应返回
		OutboxEvent evt3 = createEvent("evt-3", OutboxStatus.SENDING);
		evt3.setLockedBy("worker-1");
		evt3.setLockedAt(Instant.now()); // 刚锁定，未超时
		mapper.insert(evt3);

		insertEvent("evt-4", OutboxStatus.SENT); // 2 - 不应返回

		// When
		List<OutboxEvent> pending = mapper.selectPendingEvents(Instant.now(), 60, 100);

		// Then
		assertThat(pending).hasSize(2);
		assertThat(pending).extracting(OutboxEvent::getEventId).containsExactlyInAnyOrder("evt-1", "evt-2");
	}

	@Test
	@DisplayName("MAP-002: selectPending 遵守 next_retry_time")
	void selectPending_respects_next_retry_time() {
		// Given
		Instant now = Instant.now();
		Instant future = now.plusSeconds(3600);
		Instant past = now.minusSeconds(3600);

		OutboxEvent evt1 = createEvent("evt-1", OutboxStatus.RETRY);
		evt1.setNextRetryTime(future); // 未来，不应返回
		mapper.insert(evt1);

		OutboxEvent evt2 = createEvent("evt-2", OutboxStatus.RETRY);
		evt2.setNextRetryTime(past); // 过去，应返回
		mapper.insert(evt2);

		// When
		List<OutboxEvent> pending = mapper.selectPendingEvents(now, 60, 100);

		// Then
		assertThat(pending).hasSize(1);
		assertThat(pending.get(0).getEventId()).isEqualTo("evt-2");
	}

	@Test
	@DisplayName("MAP-003: selectPending 遵守锁超时")
	void selectPending_respects_lock_timeout() {
		// Given
		Instant now = Instant.now();

		OutboxEvent evt1 = createEvent("evt-1", OutboxStatus.NEW);
		evt1.setLockedAt(now.minusSeconds(30)); // 锁未过期（60s超时）
		mapper.insert(evt1);

		OutboxEvent evt2 = createEvent("evt-2", OutboxStatus.NEW);
		evt2.setLockedAt(now.minusSeconds(120)); // 锁已过期
		mapper.insert(evt2);

		OutboxEvent evt3 = createEvent("evt-3", OutboxStatus.NEW);
		// 无锁
		mapper.insert(evt3);

		// When
		List<OutboxEvent> pending = mapper.selectPendingEvents(now, 60, 100);

		// Then
		assertThat(pending).hasSize(2);
		assertThat(pending).extracting(OutboxEvent::getEventId).containsExactlyInAnyOrder("evt-2", "evt-3");
	}

	@Test
	@DisplayName("MAP-004: selectPending 每个 aggregate 仅取一条")
	void selectPending_one_per_aggregate() {
		// Given - 同一 aggregate 的多条事件
		OutboxEvent evt1 = createEvent("evt-1", OutboxStatus.NEW);
		evt1.setAggregateType("Order");
		evt1.setAggregateId("order-123");
		mapper.insert(evt1);

		OutboxEvent evt2 = createEvent("evt-2", OutboxStatus.NEW);
		evt2.setAggregateType("Order");
		evt2.setAggregateId("order-123");
		mapper.insert(evt2);

		OutboxEvent evt3 = createEvent("evt-3", OutboxStatus.NEW);
		evt3.setAggregateType("Order");
		evt3.setAggregateId("order-456"); // 不同 aggregate
		mapper.insert(evt3);

		// When
		List<OutboxEvent> pending = mapper.selectPendingEvents(Instant.now(), 60, 100);

		// Then
		assertThat(pending).hasSize(2); // 每个 aggregate 一条
		assertThat(pending).extracting(OutboxEvent::getEventId).contains("evt-1", "evt-3");
		// evt-1 是 order-123 的最小 id
	}

	@Test
	@DisplayName("MAP-005: selectPending 按 id 排序并限制数量")
	void selectPending_orders_by_id_and_limit() {
		// Given
		for (int i = 1; i <= 5; i++) {
			insertEvent("evt-" + i, OutboxStatus.NEW);
		}

		// When
		List<OutboxEvent> pending = mapper.selectPendingEvents(Instant.now(), 60, 2);

		// Then
		assertThat(pending).hasSize(2);
		// 应按 id 升序返回前 2 条
		assertThat(pending.get(0).getId()).isLessThan(pending.get(1).getId());
	}

	@Test
	@DisplayName("MAP-006: claimEvents 仅更新符合条件的")
	void claimEvents_only_when_eligible() {
		// Given
		OutboxEvent evt1 = createEvent("evt-1", OutboxStatus.NEW);
		mapper.insert(evt1);

		OutboxEvent evt2 = createEvent("evt-2", OutboxStatus.SENT); // 已发送，不应更新
		mapper.insert(evt2);

		OutboxEvent evt3 = createEvent("evt-3", OutboxStatus.NEW);
		evt3.setLockedAt(Instant.now().minusSeconds(30)); // 锁未过期，不应更新
		mapper.insert(evt3);

		// When
		int claimed = mapper.claimEvents(List.of(evt1.getId(), evt2.getId(), evt3.getId()), "worker-1",
				Instant.now());

		// Then
		assertThat(claimed).isEqualTo(1); // 仅 evt1
	}

	@Test
	@DisplayName("MAP-007: claimEvents 更新字段")
	void claimEvents_updates_fields() {
		// Given
		OutboxEvent evt = createEvent("evt-1", OutboxStatus.NEW);
		mapper.insert(evt);

		Instant lockTime = Instant.now();

		// When
		int claimed = mapper.claimEvents(List.of(evt.getId()), "worker-1", lockTime);

		// Then
		assertThat(claimed).isEqualTo(1);

		OutboxEvent updated = mapper.selectById(evt.getId());
		assertThat(updated.getStatus()).isEqualTo(OutboxStatus.SENDING);
		assertThat(updated.getLockedBy()).isEqualTo("worker-1");
		assertThat(updated.getLockedAt()).isCloseTo(lockTime, within(1, java.time.temporal.ChronoUnit.SECONDS));
	}

	@Test
	@DisplayName("MAP-008: claimEvents 返回更新数量")
	void claimEvents_returns_count() {
		// Given
		OutboxEvent evt1 = createEvent("evt-1", OutboxStatus.NEW);
		mapper.insert(evt1);

		OutboxEvent evt2 = createEvent("evt-2", OutboxStatus.NEW);
		mapper.insert(evt2);

		OutboxEvent evt3 = createEvent("evt-3", OutboxStatus.SENT);
		mapper.insert(evt3);

		// When
		int claimed = mapper.claimEvents(List.of(evt1.getId(), evt2.getId(), evt3.getId()), "worker-1",
				Instant.now());

		// Then
		assertThat(claimed).isEqualTo(2);
	}

	@Test
	@DisplayName("MAP-009: 唯一 event_id 约束")
	void unique_event_id_constraint() {
		// Given
		insertEvent("evt-duplicate", OutboxStatus.NEW);

		// When & Then
		assertThatThrownBy(() -> insertEvent("evt-duplicate", OutboxStatus.NEW))
			.isInstanceOf(DuplicateKeyException.class);
	}

	// 辅助方法
	private void insertEvent(String eventId, OutboxStatus status) {
		OutboxEvent event = createEvent(eventId, status);
		mapper.insert(event);
	}

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
