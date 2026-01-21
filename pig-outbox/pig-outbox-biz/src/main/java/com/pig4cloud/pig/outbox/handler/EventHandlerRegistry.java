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

package com.pig4cloud.pig.outbox.handler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件处理器注册表
 * <p>
 * 用于管理所有标注了 @DomainEventHandler 的方法
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Slf4j
@Component
public class EventHandlerRegistry {

	private final Map<String, List<HandlerMethod>> handlers = new ConcurrentHashMap<>();

	/**
	 * 注册事件处理器
	 * @param domain 领域
	 * @param eventType 事件类型
	 * @param bean Bean实例
	 * @param method 处理方法
	 * @param groupId Kafka消费者组ID
	 */
	public void register(String domain, String eventType, Object bean, Method method, String groupId) {
		String key = buildKey(domain, eventType);
		handlers.computeIfAbsent(key, k -> new ArrayList<>()).add(new HandlerMethod(bean, method, groupId));
		log.info("Registered event handler: domain={}, eventType={}, method={}.{}", domain, eventType,
				bean.getClass().getSimpleName(), method.getName());
	}

	/**
	 * 获取事件处理器
	 * @param domain 领域
	 * @param eventType 事件类型
	 * @return 处理器方法列表（副本）
	 */
	public List<HandlerMethod> getHandlers(String domain, String eventType) {
		String key = buildKey(domain, eventType);
		List<HandlerMethod> list = handlers.get(key);
		return list == null ? new ArrayList<>() : new ArrayList<>(list);
	}

	/**
	 * 获取所有已注册的处理器
	 * @return 所有处理器（副本）
	 */
	public Map<String, List<HandlerMethod>> getAllHandlers() {
		Map<String, List<HandlerMethod>> copy = new ConcurrentHashMap<>();
		handlers.forEach((key, value) -> copy.put(key, new ArrayList<>(value)));
		return copy;
	}

	private String buildKey(String domain, String eventType) {
		return domain + ":" + eventType;
	}

	/**
	 * 处理器方法包装类
	 */
	@Data
	public static class HandlerMethod {

		private final Object bean;

		private final Method method;

		private final String groupId;

		public HandlerMethod(Object bean, Method method, String groupId) {
			this.bean = bean;
			this.method = method;
			this.groupId = groupId;
		}

	}

}
