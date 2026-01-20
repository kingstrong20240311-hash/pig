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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka事件发布策略（微服务模式）
 * <p>
 * 将事件发送到Kafka消息队列
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaPublishStrategy implements EventPublishStrategy {

	// TODO: 注入KafkaTemplate或其他Kafka客户端

	@Override
	public void publish(OutboxEvent event) {
		// TODO: 实现Kafka发送逻辑
		// 示例：
		// String topic = "domain." + event.getDomain() + "." + event.getEventType();
		// kafkaTemplate.send(topic, event.getPartitionKey(), event.getPayloadJson());

		log.debug("Publishing event to Kafka: eventId={}, domain={}, eventType={}", event.getEventId(),
				event.getDomain(), event.getEventType());

		// 临时实现：仅记录日志
		log.warn("KafkaPublishStrategy not fully implemented yet, event will be marked as sent: eventId={}",
				event.getEventId());
	}

}
