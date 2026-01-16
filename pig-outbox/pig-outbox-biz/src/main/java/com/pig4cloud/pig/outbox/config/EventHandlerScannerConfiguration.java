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

package com.pig4cloud.pig.outbox.config;

import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.handler.EventHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * 事件处理器扫描配置
 * <p>
 * 启动时扫描所有标注了 @DomainEventHandler 的方法并注册
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EventHandlerScannerConfiguration implements BeanPostProcessor {

	private final EventHandlerRegistry eventHandlerRegistry;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = bean.getClass();

		// 扫描所有方法
		for (Method method : targetClass.getDeclaredMethods()) {
			DomainEventHandler annotation = AnnotationUtils.findAnnotation(method, DomainEventHandler.class);
			if (annotation != null) {
				// 注册到注册表
				eventHandlerRegistry.register(annotation.domain(), annotation.eventType(), bean, method,
						annotation.groupId());
			}
		}

		return bean;
	}

}
