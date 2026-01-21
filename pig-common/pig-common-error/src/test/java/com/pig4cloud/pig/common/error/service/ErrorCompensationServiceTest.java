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
import com.pig4cloud.pig.common.error.annotation.ErrorHandler;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import com.pig4cloud.pig.common.error.enums.ErrorRecordStatus;
import com.pig4cloud.pig.common.error.example.OrderCancelEvent;
import com.pig4cloud.pig.common.error.mapper.ErrorRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorCompensationService 测试
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Import(ErrorCompensationServiceTest.TestConfiguration.class)
class ErrorCompensationServiceTest extends BaseIntegrationTest {

	@Autowired
	private ErrorCompensationService compensationService;

	@Autowired
	private ErrorRecordService errorRecordService;

	@Autowired
	private ErrorRecordMapper errorRecordMapper;

	@Autowired
	private TestCompensationHandler testHandler;

	@BeforeEach
	void setUp() {
		// 清空测试数据 - 使用 deleteById 逐个删除或使用 selectList 后删除
		List<ErrorRecord> allRecords = errorRecordMapper.selectList(null);
		for (ErrorRecord record : allRecords) {
			errorRecordMapper.deleteById(record.getId());
		}
		testHandler.reset();
	}

	@Test
	void testCompensateSuccess() {
		// 创建错误记录
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onSuccess", event,
				new RuntimeException("Test error"));

		// 执行补偿
		boolean result = compensationService.compensate(record.getId());

		// 验证补偿成功
		assertThat(result).isTrue();
		assertThat(testHandler.getSuccessCount()).isEqualTo(1);

		// 验证记录状态
		ErrorRecord updated = errorRecordMapper.selectById(record.getId());
		assertThat(updated.getStatus()).isEqualTo(ErrorRecordStatus.RESOLVED);
	}

	@Test
	void testCompensateFailed() {
		// 创建错误记录
		OrderCancelEvent event = new OrderCancelEvent(2L, 200L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onFailure", event,
				new RuntimeException("Test error"));

		// 执行补偿（会失败）
		boolean result = compensationService.compensate(record.getId());

		// 验证补偿失败
		assertThat(result).isFalse();

		// 验证记录状态
		ErrorRecord updated = errorRecordMapper.selectById(record.getId());
		assertThat(updated.getStatus()).isEqualTo(ErrorRecordStatus.RETRYING);
		assertThat(updated.getAttempts()).isEqualTo(1);
		assertThat(updated.getNextRetryTime()).isNotNull();
		assertThat(updated.getErrorMessage()).contains("Simulated failure");
	}

	@Test
	void testCompensateWithMaxAttempts() {
		// 创建错误记录
		OrderCancelEvent event = new OrderCancelEvent(3L, 300L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onFailure", event,
				new RuntimeException("Test error"));

		// 执行多次补偿直到达到最大次数（配置为3次）
		compensationService.compensate(record.getId());
		compensationService.compensate(record.getId());
		compensationService.compensate(record.getId());

		// 验证记录状态变为 DEAD
		ErrorRecord updated = errorRecordMapper.selectById(record.getId());
		assertThat(updated.getStatus()).isEqualTo(ErrorRecordStatus.DEAD);
		assertThat(updated.getAttempts()).isEqualTo(3);
	}

	@Test
	void testCompensateBatch() {
		// 创建多个错误记录
		for (int i = 0; i < 5; i++) {
			OrderCancelEvent event = new OrderCancelEvent((long) i, (long) i * 100, "Test " + i);
			errorRecordService.record("test", "test:onSuccess", event, new RuntimeException("Test error " + i));
		}

		// 批量补偿
		int successCount = compensationService.compensateBatch("test", 10);

		// 验证补偿成功
		assertThat(successCount).isEqualTo(5);
		assertThat(testHandler.getSuccessCount()).isEqualTo(5);

		// 验证所有记录状态
		List<ErrorRecord> records = errorRecordMapper.selectList(null);
		assertThat(records).allMatch(r -> r.getStatus() == ErrorRecordStatus.RESOLVED);
	}

	@Test
	void testCompensateBatchWithDomain() {
		// 创建不同领域的错误记录
		errorRecordService.record("order", "order:onCancel", new OrderCancelEvent(1L, 100L, "Order"),
				new RuntimeException("Order error"));

		errorRecordService.record("vault", "vault:onCreate", new OrderCancelEvent(2L, 200L, "Vault"),
				new RuntimeException("Vault error"));

		// 只补偿 order 领域
		int successCount = compensationService.compensateBatch("order", 10);

		// 验证只有 order 领域的记录被尝试补偿
		assertThat(successCount).isZero(); // order 领域没有对应的处理器，所以失败

		// order 领域的记录被标记为 DEAD（因为找不到处理器）
		List<ErrorRecord> orderRecords = errorRecordMapper
			.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErrorRecord>()
				.eq(ErrorRecord::getDomain, "order"));
		assertThat(orderRecords).hasSize(1);
		assertThat(orderRecords.get(0).getStatus()).isEqualTo(ErrorRecordStatus.DEAD);

		// vault 领域的记录状态仍然是 NEW（因为没有被补偿）
		List<ErrorRecord> vaultRecords = errorRecordMapper
			.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErrorRecord>()
				.eq(ErrorRecord::getDomain, "vault"));
		assertThat(vaultRecords).hasSize(1);
		assertThat(vaultRecords.get(0).getStatus()).isEqualTo(ErrorRecordStatus.NEW);
	}

	@Test
	void testCompensateNonExistentRecord() {
		// 补偿不存在的记录
		boolean result = compensationService.compensate(99999L);

		// 验证返回 false
		assertThat(result).isFalse();
	}

	@Test
	void testCompensateResolvedRecord() {
		// 创建错误记录并补偿成功
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onSuccess", event,
				new RuntimeException("Test error"));
		compensationService.compensate(record.getId());

		// 再次补偿已解决的记录
		testHandler.reset();
		boolean result = compensationService.compensate(record.getId());

		// 验证返回 true 且不再执行处理器
		assertThat(result).isTrue();
		assertThat(testHandler.getSuccessCount()).isZero();
	}

	@Test
	void testCompensateDeadRecord() {
		// 创建错误记录
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onFailure", event,
				new RuntimeException("Test error"));

		// 执行补偿直到变为 DEAD
		for (int i = 0; i < 3; i++) {
			compensationService.compensate(record.getId());
		}

		// 再次补偿 DEAD 记录
		boolean result = compensationService.compensate(record.getId());

		// 验证返回 false
		assertThat(result).isFalse();
	}

	@Test
	void testCompensateWithNextRetryTime() {
		// 创建错误记录并补偿失败
		OrderCancelEvent event = new OrderCancelEvent(1L, 100L, "Test");
		ErrorRecord record = errorRecordService.record("test", "test:onFailure", event,
				new RuntimeException("Test error"));
		compensationService.compensate(record.getId());

		// 获取更新后的记录
		ErrorRecord updated = errorRecordMapper.selectById(record.getId());
		Instant nextRetryTime = updated.getNextRetryTime();

		// 验证下次重试时间已设置且在未来
		assertThat(nextRetryTime).isNotNull();
		assertThat(nextRetryTime).isAfter(Instant.now());
	}

	/**
	 * 测试配置
	 */
	@Configuration
	static class TestConfiguration {

		@Bean
		public TestCompensationHandler testCompensationHandler() {
			return new TestCompensationHandler();
		}

	}

	/**
	 * 测试用补偿处理器
	 */
	static class TestCompensationHandler {

		private final AtomicInteger successCount = new AtomicInteger(0);

		private final AtomicInteger failureCount = new AtomicInteger(0);

		@ErrorHandler(domain = "test", key = "onSuccess", payloadClass = OrderCancelEvent.class)
		public void handleSuccess(OrderCancelEvent event) {
			successCount.incrementAndGet();
			// 补偿成功
		}

		@ErrorHandler(domain = "test", key = "onFailure", payloadClass = OrderCancelEvent.class)
		public void handleFailure(OrderCancelEvent event) {
			failureCount.incrementAndGet();
			// 模拟补偿失败
			throw new RuntimeException("Simulated failure");
		}

		public int getSuccessCount() {
			return successCount.get();
		}

		public int getFailureCount() {
			return failureCount.get();
		}

		public void reset() {
			successCount.set(0);
			failureCount.set(0);
		}

	}

}
