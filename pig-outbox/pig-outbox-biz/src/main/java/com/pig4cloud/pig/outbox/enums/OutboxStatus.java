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

package com.pig4cloud.pig.outbox.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Outbox事件状态枚举
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Getter
public enum OutboxStatus {

	/**
	 * 新建
	 */
	NEW(0, "新建"),

	/**
	 * 发送中
	 */
	SENDING(1, "发送中"),

	/**
	 * 已发送
	 */
	SENT(2, "已发送"),

	/**
	 * 重试中
	 */
	RETRY(3, "重试中"),

	/**
	 * 死信
	 */
	DEAD(9, "死信");

	@EnumValue
	private final int code;

	private final String description;

	OutboxStatus(int code, String description) {
		this.code = code;
		this.description = description;
	}

}
