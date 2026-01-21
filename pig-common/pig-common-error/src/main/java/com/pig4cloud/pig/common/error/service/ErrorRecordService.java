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

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.error.config.ErrorRecordProperties;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import com.pig4cloud.pig.common.error.enums.ErrorRecordStatus;
import com.pig4cloud.pig.common.error.mapper.ErrorRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * 错误记录服务
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorRecordService {

	private final ErrorRecordMapper errorRecordMapper;

	private final ErrorRecordProperties properties;

	private final ObjectMapper objectMapper;

	/**
	 * 记录错误（使用对象）
	 * @param domain 领域
	 * @param handlerKey 处理器key
	 * @param payload 原始数据对象
	 * @param throwable 异常
	 * @return 错误记录
	 */
	public ErrorRecord record(String domain, String handlerKey, Object payload, Throwable throwable) {
		String payloadJson = null;
		String payloadClass = null;

		if (payload != null) {
			try {
				payloadJson = objectMapper.writeValueAsString(payload);
				payloadClass = payload.getClass().getName();
			}
			catch (JsonProcessingException e) {
				log.error("Failed to serialize payload to JSON", e);
				payloadJson = payload.toString();
			}
		}

		return record(domain, handlerKey, payloadJson, payloadClass, throwable);
	}

	/**
	 * 记录错误（使用JSON字符串）
	 * @param domain 领域
	 * @param handlerKey 处理器key
	 * @param payloadJson 原始数据JSON
	 * @param payloadClass Payload类名
	 * @param throwable 异常
	 * @return 错误记录
	 */
	public ErrorRecord record(String domain, String handlerKey, String payloadJson, String payloadClass,
			Throwable throwable) {
		String errorMessage = throwable != null ? throwable.getMessage() : null;
		String stackTrace = throwable != null ? getStackTrace(throwable) : null;

		return record(domain, handlerKey, payloadJson, payloadClass, errorMessage, stackTrace);
	}

	/**
	 * 记录错误（完整参数）
	 * @param domain 领域
	 * @param handlerKey 处理器key
	 * @param payloadJson 原始数据JSON
	 * @param payloadClass Payload类名
	 * @param errorMessage 错误消息
	 * @param stackTrace 堆栈信息
	 * @return 错误记录
	 */
	public ErrorRecord record(String domain, String handlerKey, String payloadJson, String payloadClass,
			String errorMessage, String stackTrace) {
		ErrorRecord record = new ErrorRecord();
		record.setErrorId(IdUtil.getSnowflakeNextIdStr());
		record.setDomain(domain);
		record.setHandlerKey(handlerKey);
		record.setPayloadJson(payloadJson);
		record.setPayloadClass(payloadClass);
		record.setStatus(ErrorRecordStatus.NEW);
		record.setAttempts(0);
		record.setErrorMessage(truncate(errorMessage, 500));
		record.setStackTrace(truncate(stackTrace, properties.getStackTraceMaxLength()));
		record.setCreatedAt(Instant.now());
		record.setUpdatedAt(Instant.now());

		errorRecordMapper.insert(record);
		log.info("Error recorded: id={}, domain={}, handlerKey={}", record.getId(), domain, handlerKey);

		return record;
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
