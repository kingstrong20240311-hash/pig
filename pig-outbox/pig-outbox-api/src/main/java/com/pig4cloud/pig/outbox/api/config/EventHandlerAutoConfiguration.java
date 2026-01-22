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
 * distribution and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pig.outbox.api.config;

import com.pig4cloud.pig.outbox.api.handler.EventHandlerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event Handler 自动配置
 * <p>
 * 提供 EventHandlerRegistry bean
 *
 * @author pig4cloud
 * @date 2025-01-22
 */
@Configuration
public class EventHandlerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public EventHandlerRegistry eventHandlerRegistry() {
		return new EventHandlerRegistry();
	}

}
