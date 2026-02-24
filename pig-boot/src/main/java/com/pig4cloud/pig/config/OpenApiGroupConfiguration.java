package com.pig4cloud.pig.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 单体模式 OpenAPI 分组配置
 */
@Configuration
public class OpenApiGroupConfiguration {

	@Bean
	public GroupedOpenApi authApiGroup() {
		return GroupedOpenApi.builder().group("认证模块").packagesToScan("com.pig4cloud.pig.auth").build();
	}

	@Bean
	public GroupedOpenApi upmsApiGroup() {
		return GroupedOpenApi.builder().group("用户管理模块").packagesToScan("com.pig4cloud.pig.admin").build();
	}

	@Bean
	public GroupedOpenApi codegenApiGroup() {
		return GroupedOpenApi.builder().group("代码生成模块").packagesToScan("com.pig4cloud.pig.codegen").build();
	}

	@Bean
	public GroupedOpenApi quartzApiGroup() {
		return GroupedOpenApi.builder().group("定时任务模块").packagesToScan("com.pig4cloud.pig.daemon.quartz").build();
	}

	@Bean
	public GroupedOpenApi gymApiGroup() {
		return GroupedOpenApi.builder().group("健身管理模块").packagesToScan("com.pig4cloud.pig.gym").build();
	}

}
