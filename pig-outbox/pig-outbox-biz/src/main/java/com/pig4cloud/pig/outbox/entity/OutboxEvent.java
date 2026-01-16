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

package com.pig4cloud.pig.outbox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.outbox.enums.OutboxStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Outbox事件实体
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Data
@TableName("outbox_event")
public class OutboxEvent {

	/**
	 * 自增主键
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 全局唯一事件ID
	 */
	private String eventId;

	/**
	 * 领域（order/vault/settlement）
	 */
	private String domain;

	/**
	 * 聚合类型
	 */
	private String aggregateType;

	/**
	 * 聚合ID（业务主键）
	 */
	private String aggregateId;

	/**
	 * 事件类型
	 */
	private String eventType;

	/**
	 * 负载JSON
	 */
	private String payloadJson;

	/**
	 * 头部信息JSON
	 */
	private String headersJson;

	/**
	 * 分区键（用于Kafka key，通常=aggregateId）
	 */
	private String partitionKey;

	/**
	 * 状态
	 */
	private OutboxStatus status;

	/**
	 * 尝试次数
	 */
	private Integer attempts;

	/**
	 * 下次重试时间
	 */
	private Instant nextRetryTime;

	/**
	 * 锁定者标识
	 */
	private String lockedBy;

	/**
	 * 锁定时间
	 */
	private Instant lockedAt;

	/**
	 * 创建时间
	 */
	private Instant createdAt;

	/**
	 * 更新时间
	 */
	private Instant updatedAt;

}
