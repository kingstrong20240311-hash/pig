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

package com.pig4cloud.pig.common.error;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.pig4cloud.pig.common.error.config.ErrorRecordAutoConfiguration;
import com.pig4cloud.pig.common.mybatis.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 基础集成测试类
 * <p>
 * 使用 TestContainers 提供 MySQL 数据库
 * </p>
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@SpringBootTest(classes = { ErrorRecordAutoConfiguration.class, DataSourceAutoConfiguration.class,
		MybatisPlusAutoConfiguration.class, MybatisAutoConfiguration.class, JacksonAutoConfiguration.class,
		WebMvcAutoConfiguration.class })
@Testcontainers
public abstract class BaseIntegrationTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("test")
		.withUsername("test")
		.withPassword("test")
		.withInitScript("db/error_record_init.sql");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
	}

}
