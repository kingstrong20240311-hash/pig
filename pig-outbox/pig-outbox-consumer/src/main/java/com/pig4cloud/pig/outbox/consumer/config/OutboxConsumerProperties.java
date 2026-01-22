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

package com.pig4cloud.pig.outbox.consumer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Outbox Consumer 配置属性
 *
 * @author pig4cloud
 * @date 2025-01-21
 */
@Data
@ConfigurationProperties(prefix = "pig.outbox.consumer")
public class OutboxConsumerProperties {

	/**
	 * 是否启用消费者（默认关闭，显式开启）
	 */
	private boolean enabled = false;

	/**
	 * Kafka bootstrap servers
	 */
	private String bootstrapServers = "localhost:9092";

	/**
	 * 要订阅的领域列表（例如：["order", "vault"]）
	 */
	private List<String> domains = new ArrayList<>();

	/**
	 * 消费者组ID前缀
	 */
	private String groupIdPrefix = "pig-outbox-consumer";

	/**
	 * Auto offset reset 策略
	 */
	private String autoOffsetReset = "earliest";

	/**
	 * 是否启用自动提交
	 */
	private boolean enableAutoCommit = false;

	/**
	 * Session 超时时间（毫秒）
	 */
	private int sessionTimeoutMs = 30000;

	/**
	 * 每次轮询最大记录数
	 */
	private int maxPollRecords = 100;

	/**
	 * 并发消费者数量
	 */
	private int concurrency = 3;

}
