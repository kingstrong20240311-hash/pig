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
import com.pig4cloud.pig.common.error.handler.ErrorHandlerDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误处理器注册表
 * <p>
 * 扫描并注册所有标注了@ErrorHandler的方法
 * </p>
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@Component
public class ErrorHandlerRegistry implements BeanPostProcessor {

	private final Map<String, ErrorHandlerDefinition> handlers = new ConcurrentHashMap<>();

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = bean.getClass();

		ReflectionUtils.doWithMethods(targetClass, method -> {
			ErrorHandler annotation = AnnotationUtils.findAnnotation(method, ErrorHandler.class);
			if (annotation != null) {
				registerHandler(bean, method, annotation);
			}
		});

		return bean;
	}

	/**
	 * 注册处理器
	 * @param bean Bean实例
	 * @param method 方法
	 * @param annotation 注解
	 */
	private void registerHandler(Object bean, Method method, ErrorHandler annotation) {
		String domain = annotation.domain();
		String key = StringUtils.hasText(annotation.key()) ? annotation.key() : method.getName();
		String fullHandlerKey = buildHandlerKey(domain, key);

		// 检查是否已存在
		if (handlers.containsKey(fullHandlerKey)) {
			log.warn("Duplicate error handler found: {}, existing handler will be overridden", fullHandlerKey);
		}

		Class<?> payloadClass = annotation.payloadClass();
		if (payloadClass == Void.class) {
			payloadClass = null;
		}

		ErrorHandlerDefinition definition = new ErrorHandlerDefinition(domain, key, fullHandlerKey, payloadClass, bean,
				method);

		handlers.put(fullHandlerKey, definition);
		log.info("Registered error handler: {} -> {}.{}", fullHandlerKey, bean.getClass().getSimpleName(),
				method.getName());
	}

	/**
	 * 构建handler key
	 * @param domain 领域
	 * @param key key
	 * @return 完整的handler key
	 */
	public static String buildHandlerKey(String domain, String key) {
		return domain + ":" + key;
	}

	/**
	 * 获取处理器定义
	 * @param handlerKey handler key
	 * @return 处理器定义
	 */
	public ErrorHandlerDefinition getHandler(String handlerKey) {
		return handlers.get(handlerKey);
	}

	/**
	 * 判断处理器是否存在
	 * @param handlerKey handler key
	 * @return 是否存在
	 */
	public boolean hasHandler(String handlerKey) {
		return handlers.containsKey(handlerKey);
	}

	/**
	 * 获取所有处理器
	 * @return 处理器映射
	 */
	public Map<String, ErrorHandlerDefinition> getAllHandlers() {
		return Map.copyOf(handlers);
	}

}
