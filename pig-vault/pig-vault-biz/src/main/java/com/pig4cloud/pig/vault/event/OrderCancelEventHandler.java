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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderCancelPayload;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Handle order cancellation events for vault operations
 *
 * @author pig4cloud
 * @date 2026-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancelEventHandler {

	private final VaultFreezeService vaultFreezeService;

	private final ObjectMapper objectMapper;

	/**
	 * Handle OrderCancel event - release freeze for order
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderCancel")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrderCancel(DomainEventEnvelope<OrderCancelPayload> event) {
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

	private String extractOrderId(DomainEventEnvelope<OrderCancelPayload> event) {
		OrderCancelPayload payload = event.payloadAs(objectMapper, OrderCancelPayload.class);
		if (payload != null && payload.getOrderId() != null) {
			return String.valueOf(payload.getOrderId());
		}
		return event.aggregateId();
	}

}
