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

import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import com.pig4cloud.pig.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

/**
 * Outbox事件调度器
 * <p>
 * 从数据库读取待发送事件，并根据模式调度发布
 * <p>
 * 由外部定时任务工具（如pig-quartz）定期调用dispatch()方法
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxEventDispatcher {

	private final OutboxEventMapper outboxEventMapper;

	private final EventPublishStrategy publishStrategy;

	private final OutboxDispatcherProperties properties;

	private final String nodeId;

	public OutboxEventDispatcher(OutboxEventMapper outboxEventMapper, EventPublishStrategy publishStrategy,
			OutboxDispatcherProperties properties) {
		this.outboxEventMapper = outboxEventMapper;
		this.publishStrategy = publishStrategy;
		this.properties = properties;
		this.nodeId = generateNodeId();
	}

	/**
	 * 调度任务入口，由外部定时任务调用
	 */
	public void dispatch() {
		if (!properties.isEnabled()) {
			return;
		}

		try {
			// 1. 查询待发送事件
			List<OutboxEvent> events = outboxEventMapper.selectPendingEvents(Instant.now(),
					properties.getLockTimeoutSeconds(), properties.getBatchSize());

			if (events.isEmpty()) {
				return;
			}

			log.debug("Found {} pending events to dispatch", events.size());

			// 2. 尝试锁定事件
			List<Long> eventIds = events.stream().map(OutboxEvent::getId).toList();
			int claimed = outboxEventMapper.claimEvents(eventIds, nodeId, Instant.now());

			if (claimed == 0) {
				log.debug("Failed to claim any events, likely locked by another node");
				return;
			}

			log.debug("Successfully claimed {} events", claimed);

			// 3. 重新查询已锁定的事件（确保获取最新状态）
			List<OutboxEvent> claimedEvents = events.stream()
				.filter(e -> eventIds.contains(e.getId()))
				.limit(claimed)
				.toList();

			// 4. 调度发布事件
			for (OutboxEvent event : claimedEvents) {
				dispatchEvent(event);
			}
		}
		catch (Exception e) {
			log.error("Error during event dispatch", e);
		}
	}

	/**
	 * 调度单个事件
	 */
	private void dispatchEvent(OutboxEvent event) {
		try {
			// 调用发布策略
			publishStrategy.publish(event);

			// 更新状态为已发送
			event.setStatus(OutboxStatus.SENT);
			event.setUpdatedAt(Instant.now());
			outboxEventMapper.updateById(event);

			log.debug("Event dispatched successfully: eventId={}, domain={}, eventType={}", event.getEventId(),
					event.getDomain(), event.getEventType());
		}
		catch (Exception e) {
			log.error("Failed to dispatch event: eventId={}, domain={}, eventType={}", event.getEventId(),
					event.getDomain(), event.getEventType(), e);

			// 更新重试信息
			handleDispatchFailure(event);
		}
	}

	/**
	 * 处理发送失败
	 */
	private void handleDispatchFailure(OutboxEvent event) {
		try {
			int attempts = event.getAttempts() + 1;
			event.setAttempts(attempts);

			if (attempts >= properties.getMaxRetries()) {
				// 超过最大重试次数，标记为死信
				event.setStatus(OutboxStatus.DEAD);
				log.warn("Event marked as DEAD after {} attempts: eventId={}", attempts, event.getEventId());
			}
			else {
				// 计算下次重试时间（指数退避）
				long delaySeconds = (long) Math.pow(2, attempts) * properties.getRetryDelaySeconds();
				event.setNextRetryTime(Instant.now().plusSeconds(delaySeconds));
				event.setStatus(OutboxStatus.RETRY);
				log.debug("Event will retry after {} seconds: eventId={}, attempts={}", delaySeconds,
						event.getEventId(), attempts);
			}

			event.setLockedBy(null);
			event.setLockedAt(null);
			event.setUpdatedAt(Instant.now());
			outboxEventMapper.updateById(event);
		}
		catch (Exception e) {
			log.error("Failed to update event retry info: eventId={}", event.getEventId(), e);
		}
	}

	/**
	 * 生成节点标识
	 */
	private String generateNodeId() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			long pid = ProcessHandle.current().pid();
			return String.format("%s-%d", hostname, pid);
		}
		catch (UnknownHostException e) {
			log.warn("Failed to get hostname, using fallback", e);
			return "unknown-" + ProcessHandle.current().pid();
		}
	}

}
