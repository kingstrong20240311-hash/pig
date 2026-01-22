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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox Kafka 配置属性
 *
 * @author pig4cloud
 * @date 2025-01-21
 */
@Data
@ConfigurationProperties(prefix = "pig.outbox.kafka")
public class OutboxKafkaProperties {

	/**
	 * Kafka bootstrap servers（逗号分隔）
	 */
	private String bootstrapServers = "localhost:9092";

	/**
	 * 生产者发送超时时间（秒）
	 */
	private int sendTimeoutSeconds = 30;

	/**
	 * 生产者 acks 配置（all, 1, 0）
	 */
	private String acks = "all";

	/**
	 * 生产者重试次数（需要非零值以支持幂等性）
	 */
	private int retries = 3;

	/**
	 * 生产者最大在途请求数
	 */
	private int maxInFlightRequestsPerConnection = 5;

	/**
	 * 启用幂等性以保证精确一次语义
	 */
	private boolean enableIdempotence = true;

}
