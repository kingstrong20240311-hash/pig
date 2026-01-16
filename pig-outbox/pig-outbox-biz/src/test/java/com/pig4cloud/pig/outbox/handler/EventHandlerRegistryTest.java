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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EventHandlerRegistry 单元测试
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@DisplayName("EventHandlerRegistry 测试")
class EventHandlerRegistryTest {

	private EventHandlerRegistry registry;

	private Object beanA;

	private Object beanB;

	private Method methodA;

	private Method methodB;

	@BeforeEach
	void setUp() throws Exception {
		registry = new EventHandlerRegistry();
		beanA = new TestHandler();
		beanB = new TestHandler();
		methodA = TestHandler.class.getMethod("handleA");
		methodB = TestHandler.class.getMethod("handleB");
	}

	@Test
	@DisplayName("REG-001: getHandlers 未注册时返回空列表")
	void getHandlers_returns_empty_when_missing() {
		// When
		List<EventHandlerRegistry.HandlerMethod> handlers = registry.getHandlers("order", "OrderCreated");

		// Then
		assertThat(handlers).isEmpty();
	}

	@Test
	@DisplayName("REG-002: register 保持注册顺序")
	void register_keeps_order() {
		// Given
		registry.register("order", "OrderMatched", beanA, methodA, "group-a");
		registry.register("order", "OrderMatched", beanB, methodB, "group-b");

		// When
		List<EventHandlerRegistry.HandlerMethod> handlers = registry.getHandlers("order", "OrderMatched");

		// Then
		assertThat(handlers).hasSize(2);
		assertThat(handlers.get(0).getBean()).isSameAs(beanA);
		assertThat(handlers.get(0).getMethod()).isEqualTo(methodA);
		assertThat(handlers.get(1).getBean()).isSameAs(beanB);
		assertThat(handlers.get(1).getMethod()).isEqualTo(methodB);
	}

	@Test
	@DisplayName("REG-003: getHandlers 返回副本")
	void getHandlers_returns_copy() {
		// Given
		registry.register("order", "OrderMatched", beanA, methodA, "group-a");

		// When
		List<EventHandlerRegistry.HandlerMethod> handlers1 = registry.getHandlers("order", "OrderMatched");
		handlers1.clear(); // 修改返回的列表

		List<EventHandlerRegistry.HandlerMethod> handlers2 = registry.getHandlers("order", "OrderMatched");

		// Then
		assertThat(handlers2).hasSize(1); // 不受影响
	}

	@Test
	@DisplayName("REG-004: getAllHandlers 返回副本")
	void getAllHandlers_returns_copy() {
		// Given
		registry.register("order", "OrderMatched", beanA, methodA, "group-a");

		// When
		Map<String, List<EventHandlerRegistry.HandlerMethod>> all1 = registry.getAllHandlers();
		all1.clear(); // 修改返回的 map

		Map<String, List<EventHandlerRegistry.HandlerMethod>> all2 = registry.getAllHandlers();

		// Then
		assertThat(all2).hasSize(1); // 不受影响
	}

	// 测试辅助类
	static class TestHandler {

		public void handleA() {
		}

		public void handleB() {
		}

	}

}
