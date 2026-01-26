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

package com.pig4cloud.pig.vault.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Map;


/**
 * Handle order cancellation events for vault operations
 *
 * @author pig4cloud
 * @date 2026-01-25
 */
@Service
@RequiredArgsConstructor
public class OrderCancelEventHandler {

	private static final Logger log = LoggerFactory.getLogger(OrderCancelEventHandler.class);

	private final VaultFreezeService vaultFreezeService;

	private final ObjectMapper objectMapper;

	/**
	 * Handle OrderCancel event - release freeze for order
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderCancel")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrderCancel(DomainEventEnvelope event) {
		log.info("Handling OrderCancel event in vault: eventId={}, aggregateId={}", event.eventId(),
				event.aggregateId());

		try {
			String orderId = extractOrderId(event);
			if (orderId == null || orderId.isBlank()) {
				log.error("Failed to extract orderId from event payload: eventId={}, payload={}", event.eventId(),
						event.payloadJson());
				throw new IllegalArgumentException("Invalid payload: orderId not found");
			}

			FreezeLookupRequest request = new FreezeLookupRequest();
			request.setRefType(RefType.ORDER);
			request.setRefId(orderId);

			vaultFreezeService.releaseFreeze(request);

			log.info("Freeze released for order: orderId={}, eventId={}", orderId, event.eventId());
		}
		catch (Exception e) {
			log.error("Failed to handle OrderCancel event in vault: eventId={}, aggregateId={}", event.eventId(),
					event.aggregateId(), e);
			throw new RuntimeException("Failed to process OrderCancel event in vault", e);
		}
	}

	private String extractOrderId(DomainEventEnvelope event) {
		Map<String, Object> payload = extractPayloadMap(event);
		if (payload != null && payload.get("orderId") != null) {
			return String.valueOf(payload.get("orderId"));
		}
		return extractAggregateId(event);
	}

	private Map<String, Object> extractPayloadMap(DomainEventEnvelope event) {
		Object payload = invokeMethod(event, "payload");
		if (payload != null) {
			return objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {
			});
		}
		String payloadJson = extractPayloadJson(event);
		if (payloadJson == null || payloadJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse payloadJson", e);
		}
	}

	private String extractPayloadJson(DomainEventEnvelope event) {
		String payloadJson = toStringOrNull(invokeMethod(event, "payloadJson"));
		if (payloadJson != null) {
			return payloadJson;
		}
		return toStringOrNull(invokeMethod(event, "getPayloadJson"));
	}

	private String extractAggregateId(DomainEventEnvelope event) {
		String aggregateId = toStringOrNull(invokeMethod(event, "aggregateId"));
		if (aggregateId != null) {
			return aggregateId;
		}
		return toStringOrNull(invokeMethod(event, "getAggregateId"));
	}

	private Object invokeMethod(Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		}
		catch (Exception e) {
			return null;
		}
	}

	private String toStringOrNull(Object value) {
		return value == null ? null : String.valueOf(value);
	}

}
