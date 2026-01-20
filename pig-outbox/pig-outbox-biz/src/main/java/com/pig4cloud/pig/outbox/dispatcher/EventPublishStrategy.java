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

package com.pig4cloud.pig.outbox.dispatcher;

import com.pig4cloud.pig.outbox.entity.OutboxEvent;

/**
 * 事件发布策略接口
 *
 * @author pig4cloud
 * @date 2025-01-20
 */
public interface EventPublishStrategy {

	/**
	 * 发布事件
	 * @param event 待发布事件
	 */
	void publish(OutboxEvent event);

}
