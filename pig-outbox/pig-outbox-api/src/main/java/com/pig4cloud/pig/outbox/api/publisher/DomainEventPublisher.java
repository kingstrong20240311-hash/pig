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

package com.pig4cloud.pig.outbox.api.publisher;

import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;

/**
 * 领域事件发布器统一接口
 * <p>
 * 业务代码只依赖此接口，运行时按profile切换实现： - 单体模式：InProcessEventPublisher（进程内总线） -
 * 微服务模式：DbOutboxEventPublisher（写MySQL outbox表）
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
public interface DomainEventPublisher {

	/**
	 * 发布领域事件
	 * @param event 领域事件信封
	 */
	void publish(DomainEventEnvelope event);

}
