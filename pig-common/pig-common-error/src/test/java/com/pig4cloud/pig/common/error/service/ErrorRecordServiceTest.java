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

package com.pig4cloud.pig.common.error.service;

import com.pig4cloud.pig.common.error.BaseIntegrationTest;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import com.pig4cloud.pig.common.error.enums.ErrorRecordStatus;
import com.pig4cloud.pig.common.error.example.OrderCancelEvent;
import com.pig4cloud.pig.common.error.mapper.ErrorRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorRecordService 测试
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
class ErrorRecordServiceTest extends BaseIntegrationTest {

	@Autowired
	private ErrorRecordService errorRecordService;

	@Autowired
	private ErrorRecordMapper errorRecordMapper;

	@Test
	void testRecordWithObject() {
		// 准备测试数据
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "User cancelled");
		Exception exception = new RuntimeException("Test exception");

		// 记录错误
		ErrorRecord record = errorRecordService.record("order", "order:onCancel", event, exception);

		// 验证记录
		assertThat(record).isNotNull();
		assertThat(record.getId()).isNotNull();
		assertThat(record.getErrorId()).isNotNull();
		assertThat(record.getDomain()).isEqualTo("order");
		assertThat(record.getHandlerKey()).isEqualTo("order:onCancel");
		assertThat(record.getPayloadJson()).contains("\"orderId\":1");
		assertThat(record.getPayloadClass()).isEqualTo(OrderCancelEvent.class.getName());
		assertThat(record.getStatus()).isEqualTo(ErrorRecordStatus.NEW);
		assertThat(record.getAttempts()).isZero();
		assertThat(record.getErrorMessage()).isEqualTo("Test exception");
		assertThat(record.getStackTrace()).contains("RuntimeException");
		assertThat(record.getCreatedAt()).isNotNull();
		assertThat(record.getUpdatedAt()).isNotNull();

		// 验证数据库中存在
		ErrorRecord dbRecord = errorRecordMapper.selectById(record.getId());
		assertThat(dbRecord).isNotNull();
		assertThat(dbRecord.getDomain()).isEqualTo("order");
	}

	@Test
	void testRecordWithJsonString() {
		// 准备测试数据
		String payloadJson = "{\"orderId\":2,\"userId\":200,\"reason\":\"Test\"}";
		Exception exception = new IllegalArgumentException("Invalid argument");

		// 记录错误
		ErrorRecord record = errorRecordService.record("vault", "vault:onCreate", payloadJson, "com.example.VaultEvent",
				exception);

		// 验证记录
		assertThat(record).isNotNull();
		assertThat(record.getDomain()).isEqualTo("vault");
		assertThat(record.getHandlerKey()).isEqualTo("vault:onCreate");
		assertThat(record.getPayloadJson()).isEqualTo(payloadJson);
		assertThat(record.getPayloadClass()).isEqualTo("com.example.VaultEvent");
		assertThat(record.getStatus()).isEqualTo(ErrorRecordStatus.NEW);
		assertThat(record.getErrorMessage()).isEqualTo("Invalid argument");
	}

	@Test
	void testRecordWithCustomErrorMessage() {
		// 准备测试数据
		String payloadJson = "{\"test\":\"data\"}";
		String errorMessage = "Custom error message";
		String stackTrace = "Custom stack trace";

		// 记录错误
		ErrorRecord record = errorRecordService.record("settlement", "settlement:process", payloadJson, null,
				errorMessage, stackTrace);

		// 验证记录
		assertThat(record).isNotNull();
		assertThat(record.getDomain()).isEqualTo("settlement");
		assertThat(record.getHandlerKey()).isEqualTo("settlement:process");
		assertThat(record.getErrorMessage()).isEqualTo(errorMessage);
		assertThat(record.getStackTrace()).isEqualTo(stackTrace);
	}

	@Test
	void testRecordWithLongStackTrace() {
		// 准备一个很长的堆栈信息
		StringBuilder longStackTrace = new StringBuilder();
		for (int i = 0; i < 200; i++) {
			longStackTrace.append("at com.example.Class.method")
				.append(i)
				.append("(File.java:")
				.append(i)
				.append(")\n");
		}

		Exception exception = new Exception(longStackTrace.toString());

		// 记录错误
		ErrorRecord record = errorRecordService.record("test", "test:handler", "test data", null, exception);

		// 验证堆栈信息被截断
		assertThat(record.getStackTrace()).isNotNull();
		assertThat(record.getStackTrace().length()).isLessThanOrEqualTo(1000); // 配置的最大长度
	}

	@Test
	void testRecordWithNullPayload() {
		// 记录错误时不传递 payload
		Exception exception = new RuntimeException("No payload");

		ErrorRecord record = errorRecordService.record("test", "test:noPayload", (Object) null, exception);

		// 验证记录
		assertThat(record).isNotNull();
		assertThat(record.getPayloadJson()).isNull();
		assertThat(record.getPayloadClass()).isNull();
		assertThat(record.getErrorMessage()).isEqualTo("No payload");
	}

}
