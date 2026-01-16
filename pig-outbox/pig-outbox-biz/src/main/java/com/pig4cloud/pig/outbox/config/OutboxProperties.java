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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbox配置属性
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Data
@ConfigurationProperties(prefix = "pig.outbox")
public class OutboxProperties {

	/**
	 * 运行模式：monolithic（单体）, microservice（微服务）
	 */
	private Mode mode = Mode.MICROSERVICE;

	/**
	 * 运行模式枚举
	 */
	public enum Mode {

		/**
		 * 单体模式：进程内事件总线
		 */
		MONOLITHIC,

		/**
		 * 微服务模式：数据库Outbox + Kafka
		 */
		MICROSERVICE

	}

}
