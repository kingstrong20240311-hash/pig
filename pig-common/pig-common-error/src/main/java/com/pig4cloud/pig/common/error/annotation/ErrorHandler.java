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

package com.pig4cloud.pig.common.error.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 错误处理器注解
 * <p>
 * 标注在处理方法上，用于注册错误补偿处理器
 * </p>
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ErrorHandler {

	/**
	 * 领域（order/vault/settlement等）
	 * @return 领域名称
	 */
	String domain();

	/**
	 * 处理器唯一标识key（可选，默认使用方法名）
	 * @return 处理器key
	 */
	String key() default "";

	/**
	 * Payload反序列化类型（可选）
	 * @return 反序列化类型
	 */
	Class<?> payloadClass() default Void.class;

}
