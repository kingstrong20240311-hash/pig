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

package com.pig4cloud.pig.outbox.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 领域事件处理器注解
 * <p>
 * 统一监听注解，单体与微服务都支持： - 单体模式：扫描注册到 InProcessEventBus - 微服务模式：扫描创建 @KafkaListener
 * 或统一Listener分发
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainEventHandler {

	/**
	 * 领域（order/vault/settlement）
	 */
	String domain();

	/**
	 * 事件类型（OrderMatched/VaultCreated...）
	 */
	String eventType();

	/**
	 * Kafka消费者组ID（微服务模式使用）
	 */
	String groupId() default "";

}
