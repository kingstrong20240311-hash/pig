/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pig4cloud.pig.common.error.registry;

import com.pig4cloud.pig.common.error.annotation.ErrorHandler;
import com.pig4cloud.pig.common.error.example.OrderCancelEvent;
import com.pig4cloud.pig.common.error.handler.ErrorHandlerDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorHandlerRegistry 测试
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@SpringBootTest(classes = ErrorHandlerRegistryTest.TestConfiguration.class)
class ErrorHandlerRegistryTest {

	@Autowired
	private ErrorHandlerRegistry registry;

	@Test
	void testHandlerRegistration() {
		// 验证处理器已注册
		assertThat(registry.hasHandler("test:handleWithPayload")).isTrue();
		assertThat(registry.hasHandler("test:handleWithJson")).isTrue();
		assertThat(registry.hasHandler("test:handleWithoutParam")).isTrue();

		// 验证不存在的处理器
		assertThat(registry.hasHandler("test:nonexistent")).isFalse();
	}

	@Test
	void testGetHandler() {
		// 获取处理器定义
		ErrorHandlerDefinition definition = registry.getHandler("test:handleWithPayload");

		assertThat(definition).isNotNull();
		assertThat(definition.getDomain()).isEqualTo("test");
		assertThat(definition.getKey()).isEqualTo("handleWithPayload");
		assertThat(definition.getFullHandlerKey()).isEqualTo("test:handleWithPayload");
		assertThat(definition.getPayloadClass()).isEqualTo(OrderCancelEvent.class);
		assertThat(definition.getMethod()).isNotNull();
		assertThat(definition.getBean()).isNotNull();
	}

	@Test
	void testGetHandlerWithoutPayloadClass() {
		// 获取没有指定 payloadClass 的处理器
		ErrorHandlerDefinition definition = registry.getHandler("test:handleWithJson");

		assertThat(definition).isNotNull();
		assertThat(definition.getPayloadClass()).isNull();
	}

	@Test
	void testBuildHandlerKey() {
		String handlerKey = ErrorHandlerRegistry.buildHandlerKey("order", "onCancel");
		assertThat(handlerKey).isEqualTo("order:onCancel");
	}

	@Test
	void testGetAllHandlers() {
		var allHandlers = registry.getAllHandlers();

		assertThat(allHandlers).isNotEmpty();
		assertThat(allHandlers).containsKey("test:handleWithPayload");
		assertThat(allHandlers).containsKey("test:handleWithJson");
		assertThat(allHandlers).containsKey("test:handleWithoutParam");
	}

	/**
	 * 测试配置
	 */
	@Configuration
	@Import(ErrorHandlerRegistry.class)
	static class TestConfiguration {

		@Bean
		public TestErrorHandler testErrorHandler() {
			return new TestErrorHandler();
		}

	}

	/**
	 * 测试用错误处理器
	 */
	static class TestErrorHandler {

		@ErrorHandler(domain = "test", key = "handleWithPayload", payloadClass = OrderCancelEvent.class)
		public void handleWithPayload(OrderCancelEvent event) {
			// 测试方法
		}

		@ErrorHandler(domain = "test", key = "handleWithJson")
		public void handleWithJson(String json) {
			// 测试方法
		}

		@ErrorHandler(domain = "test", key = "handleWithoutParam")
		public void handleWithoutParam() {
			// 测试方法
		}

	}

}
