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

package com.pig4cloud.pig.outbox.api.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 领域事件信封 - 统一事件结构
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
public record DomainEventEnvelope(String eventId, // 全局唯一（UUID/ULID）
		String domain, // "order" / "vault" / "settlement"
		String aggregateType, // "Order" / "VaultAccount" / "SettlementIntent"
		String aggregateId, // 业务主键（用于保序 & Kafka key）
		String eventType, // "OrderMatched" ...
		Instant occurredAt, Map<String, String> headers, String payloadJson // 事件负载JSON
) implements Serializable {
}
