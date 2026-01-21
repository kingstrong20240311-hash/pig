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

package com.pig4cloud.pig.common.error.controller;

import com.pig4cloud.pig.common.error.BaseIntegrationTest;
import com.pig4cloud.pig.common.error.annotation.ErrorHandler;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import com.pig4cloud.pig.common.error.example.OrderCancelEvent;
import com.pig4cloud.pig.common.error.mapper.ErrorRecordMapper;
import com.pig4cloud.pig.common.error.service.ErrorRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ErrorRecordController 集成测试
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@AutoConfigureMockMvc
@Import(ErrorRecordControllerTest.TestConfiguration.class)
class ErrorRecordControllerTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ErrorRecordService errorRecordService;

	@Autowired
	private ErrorRecordMapper errorRecordMapper;

	@BeforeEach
	void setUp() {
		// 清空测试数据
		java.util.List<ErrorRecord> allRecords = errorRecordMapper.selectList(null);
		for (ErrorRecord record : allRecords) {
			errorRecordMapper.deleteById(record.getId());
		}
	}

	@Test
	void testCompensate() throws Exception {
		// 创建错误记录
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onSuccess", event,
				new RuntimeException("Test error"));

		// 执行补偿
		mockMvc.perform(post("/error/records/{id}/compensate", record.getId()).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data").value(true));
	}

	@Test
	void testCompensateBatch() throws Exception {
		// 创建多个错误记录
		for (int i = 0; i < 3; i++) {
			OrderCancelEvent event = new OrderCancelEvent((long) i, (long) i * 100, "Test " + i);
			errorRecordService.record("test", "test:onSuccess", event, new RuntimeException("Test error " + i));
		}

		// 批量补偿
		mockMvc
			.perform(post("/error/records/compensate-batch").param("domain", "test")
				.param("limit", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data").value(3));
	}

	@Test
	void testGetById() throws Exception {
		// 创建错误记录
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onSuccess", event,
				new RuntimeException("Test error"));

		// 查询记录
		mockMvc.perform(get("/error/records/{id}", record.getId()).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.id").value(record.getId()))
			.andExpect(jsonPath("$.data.domain").value("test"))
			.andExpect(jsonPath("$.data.handlerKey").value("test:onSuccess"));
	}

	@Test
	void testPage() throws Exception {
		// 创建多个错误记录
		for (int i = 0; i < 5; i++) {
			OrderCancelEvent event = new OrderCancelEvent((long) i, (long) i * 100, "Test " + i);
			errorRecordService.record("test", "test:handler" + i, event, new RuntimeException("Test error " + i));
		}

		// 分页查询
		mockMvc
			.perform(get("/error/records/page").param("page", "1")
				.param("size", "10")
				.param("domain", "test")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.records").isArray())
			.andExpect(jsonPath("$.data.total").value(5));
	}

	@Test
	void testPageWithStatus() throws Exception {
		// 创建不同状态的错误记录
		// 第一条记录 - 保持 NEW 状态（不补偿）
		OrderCancelEvent event1 = new OrderCancelEvent(1L, 100L, "Test 1");
		errorRecordService.record("test", "test:onSuccess", event1, new RuntimeException("Test error 1"));

		// 第二条记录 - 将被补偿为 RESOLVED 状态
		OrderCancelEvent event2 = new OrderCancelEvent(2L, 200L, "Test 2");
		ErrorRecord record2 = errorRecordService.record("test", "test:onSuccess", event2,
				new RuntimeException("Test error 2"));

		// 补偿第二个记录使其状态变为 RESOLVED
		mockMvc.perform(post("/error/records/{id}/compensate", record2.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data").value(true));

		// 查询 NEW 状态的记录（应该只有第一条）
		mockMvc
			.perform(get("/error/records/page").param("page", "1")
				.param("size", "10")
				.param("status", "NEW")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.total").value(1));

		// 查询 RESOLVED 状态的记录（应该只有第二条）
		mockMvc
			.perform(get("/error/records/page").param("page", "1")
				.param("size", "10")
				.param("status", "RESOLVED")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.total").value(1));
	}

	@Test
	void testGetByIdNotFound() throws Exception {
		// 查询不存在的记录
		mockMvc.perform(get("/error/records/{id}", 99999L).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1))
			.andExpect(jsonPath("$.msg").value("记录不存在"));
	}

	@Test
	void testCompensateNotFound() throws Exception {
		// 补偿不存在的记录
		mockMvc.perform(post("/error/records/{id}/compensate", 99999L).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1))
			.andExpect(jsonPath("$.msg").value("补偿失败"));
	}

	/**
	 * 测试配置
	 */
	@Configuration
	static class TestConfiguration {

		@Bean
		public TestHandler testHandler() {
			return new TestHandler();
		}

	}

	/**
	 * 测试用处理器
	 */
	static class TestHandler {

		@ErrorHandler(domain = "test", key = "onSuccess", payloadClass = OrderCancelEvent.class)
		public void handleSuccess(OrderCancelEvent event) {
			// 成功处理
		}

	}

}
