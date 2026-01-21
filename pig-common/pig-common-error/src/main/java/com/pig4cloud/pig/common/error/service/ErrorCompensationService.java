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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.error.config.ErrorRecordProperties;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import com.pig4cloud.pig.common.error.enums.ErrorRecordStatus;
import com.pig4cloud.pig.common.error.handler.ErrorHandlerDefinition;
import com.pig4cloud.pig.common.error.mapper.ErrorRecordMapper;
import com.pig4cloud.pig.common.error.registry.ErrorHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 错误补偿服务
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorCompensationService {

	private final ErrorRecordMapper errorRecordMapper;

	private final ErrorHandlerRegistry handlerRegistry;

	private final ErrorRecordProperties properties;

	private final ObjectMapper objectMapper;

	/**
	 * 单条补偿
	 * @param id 错误记录ID
	 * @return 是否成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean compensate(Long id) {
		ErrorRecord record = errorRecordMapper.selectById(id);
		if (record == null) {
			log.warn("Error record not found: id={}", id);
			return false;
		}

		// 检查状态
		if (record.getStatus() == ErrorRecordStatus.RESOLVED) {
			log.info("Error record already resolved: id={}", id);
			return true;
		}

		if (record.getStatus() == ErrorRecordStatus.DEAD) {
			log.warn("Error record is dead: id={}", id);
			return false;
		}

		return executeCompensation(record);
	}

	/**
	 * 批量补偿
	 * @param domain 领域（可选）
	 * @param limit 限制数量
	 * @return 成功数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int compensateBatch(String domain, int limit) {
		List<ErrorRecord> records = errorRecordMapper.selectPendingRecords(domain, limit, Instant.now());

		int successCount = 0;
		for (ErrorRecord record : records) {
			try {
				if (executeCompensation(record)) {
					successCount++;
				}
			}
			catch (Exception e) {
				log.error("Failed to compensate error record: id={}", record.getId(), e);
			}
		}

		log.info("Batch compensation completed: total={}, success={}", records.size(), successCount);
		return successCount;
	}

	/**
	 * 执行补偿
	 * @param record 错误记录
	 * @return 是否成功
	 */
	private boolean executeCompensation(ErrorRecord record) {
		String handlerKey = record.getHandlerKey();
		ErrorHandlerDefinition handler = handlerRegistry.getHandler(handlerKey);

		if (handler == null) {
			log.error("Handler not found: handlerKey={}", handlerKey);
			markAsDead(record, "Handler not found: " + handlerKey);
			return false;
		}

		// 更新状态为RETRYING
		record.setStatus(ErrorRecordStatus.RETRYING);
		record.setUpdatedAt(Instant.now());
		errorRecordMapper.updateById(record);

		try {
			// 准备参数
			Object arg = prepareArgument(record, handler);

			// 调用处理方法
			Method method = handler.getMethod();
			method.setAccessible(true);

			if (arg != null) {
				method.invoke(handler.getBean(), arg);
			}
			else {
				method.invoke(handler.getBean());
			}

			// 成功，标记为RESOLVED
			record.setStatus(ErrorRecordStatus.RESOLVED);
			record.setUpdatedAt(Instant.now());
			errorRecordMapper.updateById(record);

			log.info("Error compensation succeeded: id={}, handlerKey={}", record.getId(), handlerKey);
			return true;
		}
		catch (Exception e) {
			log.error("Error compensation failed: id={}, handlerKey={}", record.getId(), handlerKey, e);
			handleCompensationFailure(record, e);
			return false;
		}
	}

	/**
	 * 准备方法参数
	 * @param record 错误记录
	 * @param handler 处理器定义
	 * @return 参数对象
	 */
	private Object prepareArgument(ErrorRecord record, ErrorHandlerDefinition handler) throws Exception {
		String payloadJson = record.getPayloadJson();
		if (!StringUtils.hasText(payloadJson)) {
			return null;
		}

		Class<?> payloadClass = handler.getPayloadClass();
		if (payloadClass == null) {
			// 使用记录中的类名
			String className = record.getPayloadClass();
			if (StringUtils.hasText(className)) {
				payloadClass = Class.forName(className);
			}
		}

		if (payloadClass != null) {
			return objectMapper.readValue(payloadJson, payloadClass);
		}

		// 返回JSON字符串
		return payloadJson;
	}

	/**
	 * 处理补偿失败
	 * @param record 错误记录
	 * @param e 异常
	 */
	private void handleCompensationFailure(ErrorRecord record, Exception e) {
		// 提取实际的异常（处理反射调用时的 InvocationTargetException）
		Throwable actualException = e.getCause() != null ? e.getCause() : e;
		
		int attempts = record.getAttempts() + 1;
		record.setAttempts(attempts);
		record.setErrorMessage(truncate(actualException.getMessage(), 500));
		record.setStackTrace(truncate(getStackTrace(actualException), properties.getStackTraceMaxLength()));
		record.setLastErrorAt(Instant.now());
		record.setUpdatedAt(Instant.now());

		// 检查是否超过最大重试次数
		if (attempts >= properties.getMaxAttempts()) {
			markAsDead(record, "Max attempts reached: " + attempts);
		}
		else {
			// 计算下次重试时间
			long delaySeconds = calculateRetryDelay(attempts);
			record.setNextRetryTime(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
			record.setStatus(ErrorRecordStatus.RETRYING);

			errorRecordMapper.updateById(record);
			log.info("Error compensation will retry: id={}, attempts={}, nextRetry={}", record.getId(), attempts,
					record.getNextRetryTime());
		}
	}

	/**
	 * 标记为死信
	 * @param record 错误记录
	 * @param reason 原因
	 */
	private void markAsDead(ErrorRecord record, String reason) {
		record.setStatus(ErrorRecordStatus.DEAD);
		record.setErrorMessage(truncate(reason, 500));
		record.setUpdatedAt(Instant.now());
		errorRecordMapper.updateById(record);

		log.warn("Error record marked as DEAD: id={}, reason={}", record.getId(), reason);
	}

	/**
	 * 计算重试延迟
	 * @param attempts 尝试次数
	 * @return 延迟秒数
	 */
	private long calculateRetryDelay(int attempts) {
		if (!properties.isUseExponentialBackoff()) {
			return properties.getRetryDelaySeconds();
		}

		// 指数退避: delay * (multiplier ^ (attempts - 1))
		double delay = properties.getRetryDelaySeconds()
				* Math.pow(properties.getExponentialBackoffMultiplier(), attempts - 1);

		return (long) Math.min(delay, 3600); // 最大1小时
	}

	/**
	 * 获取堆栈信息
	 * @param throwable 异常
	 * @return 堆栈字符串
	 */
	private String getStackTrace(Throwable throwable) {
		if (throwable == null) {
			return null;
		}

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}

	/**
	 * 截断字符串
	 * @param str 原字符串
	 * @param maxLength 最大长度
	 * @return 截断后的字符串
	 */
	private String truncate(String str, int maxLength) {
		if (!StringUtils.hasText(str)) {
			return str;
		}
		if (str.length() <= maxLength) {
			return str;
		}
		return str.substring(0, maxLength);
	}

}
