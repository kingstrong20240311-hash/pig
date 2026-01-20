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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox调度器配置属性
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
@Data
@ConfigurationProperties(prefix = "pig.outbox.dispatcher")
public class OutboxDispatcherProperties {

	/**
	 * 是否启用调度器
	 */
	private boolean enabled = true;

	/**
	 * 每次批量处理数量
	 */
	private int batchSize = 100;

	/**
	 * 锁超时时间（秒）
	 */
	private int lockTimeoutSeconds = 60;

	/**
	 * 最大重试次数
	 */
	private int maxRetries = 5;

	/**
	 * 重试延迟基数（秒）
	 */
	private int retryDelaySeconds = 10;

}
