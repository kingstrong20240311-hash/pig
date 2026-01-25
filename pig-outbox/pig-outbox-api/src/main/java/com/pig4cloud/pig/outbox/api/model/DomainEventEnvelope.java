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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;

/**
 * 领域事件信封 - 统一事件结构
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DomainEventEnvelope<T>(String eventId, // 全局唯一（UUID/ULID）
		String domain, // "order" / "vault" / "settlement"
		String aggregateType, // "Order" / "VaultAccount" / "SettlementIntent"
		String aggregateId, // 业务主键（用于保序 & Kafka key）
		String eventType, // "OrderMatched" ...
		Long occurredAt, Map<String, String> headers, T payload, String payloadJson // 事件负载
) implements Serializable {

	public DomainEventEnvelope {
		if (domain == null || domain.isBlank()) {
			throw new IllegalArgumentException("domain is required");
		}
		if (eventType == null || eventType.isBlank()) {
			throw new IllegalArgumentException("eventType is required");
		}
		if (aggregateId == null || aggregateId.isBlank()) {
			throw new IllegalArgumentException("aggregateId is required");
		}
	}

	public DomainEventEnvelope(String eventId, String domain, String aggregateType, String aggregateId,
			String eventType, Long occurredAt, Map<String, String> headers, String payloadJson) {
		this(eventId, domain, aggregateType, aggregateId, eventType, occurredAt, headers, null, payloadJson);
	}

	public DomainEventEnvelope(String eventId, String domain, String aggregateType, String aggregateId,
			String eventType, Long occurredAt, Map<String, String> headers, T payload) {
		this(eventId, domain, aggregateType, aggregateId, eventType, occurredAt, headers, payload, null);
	}

	public <P> P payloadAs(ObjectMapper objectMapper, Class<P> payloadType) {
		if (objectMapper == null) {
			throw new IllegalArgumentException("objectMapper is required");
		}
		if (payloadType == null) {
			throw new IllegalArgumentException("payloadType is required");
		}
		if (payload != null) {
			if (payloadType.isInstance(payload)) {
				return payloadType.cast(payload);
			}
			return objectMapper.convertValue(payload, payloadType);
		}
		if (payloadJson == null || payloadJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(payloadJson, payloadType);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize payloadJson", e);
		}
	}
}
