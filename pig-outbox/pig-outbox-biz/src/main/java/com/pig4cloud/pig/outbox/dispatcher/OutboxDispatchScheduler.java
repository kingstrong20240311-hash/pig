/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pig.outbox.dispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox调度任务（单线程）
 *
 * @author pig4cloud
 * @date 2026-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pig.outbox.dispatcher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatchScheduler {

	private final OutboxEventDispatcher dispatcher;

	@Scheduled(scheduler = "outboxScheduler", fixedDelayString = "${pig.outbox.dispatcher.dispatch-interval-ms:1000}",
			initialDelayString = "${pig.outbox.dispatcher.initial-delay-ms:1000}")
	public void dispatch() {
		dispatcher.dispatch();
	}

}
