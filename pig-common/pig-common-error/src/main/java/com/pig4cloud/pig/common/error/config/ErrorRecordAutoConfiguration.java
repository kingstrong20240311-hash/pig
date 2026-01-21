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

package com.pig4cloud.pig.common.error.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 错误记录自动配置
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ErrorRecordProperties.class)
@ComponentScan(basePackages = "com.pig4cloud.pig.common.error",
		excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.example\\..*"))
@MapperScan("com.pig4cloud.pig.common.error.mapper")
public class ErrorRecordAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	public ErrorRecordAutoConfiguration() {
		log.info("pig-common-error module loaded");
	}

}
