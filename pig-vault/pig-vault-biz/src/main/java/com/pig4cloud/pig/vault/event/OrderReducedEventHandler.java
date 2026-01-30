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
import com.pig4cloud.pig.outbox.api.payload.order.OrderReducedPayload;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handle OrderReduced events - release (partial or full) freeze for order
 *
 * @author pig4cloud
 * @date 2026-01-30
 */
@Service
@RequiredArgsConstructor
public class OrderReducedEventHandler {

	private static final Logger log = LoggerFactory.getLogger(OrderReducedEventHandler.class);

	private final VaultFreezeService vaultFreezeService;

	private final ObjectMapper objectMapper;

	/**
	 * Handle OrderReduced event - release freeze amount for order
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderReduced")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrderReduced(DomainEventEnvelope<?> event) {
		log.info("Handling OrderReduced event in vault: eventId={}, aggregateId={}", event.eventId(),
				event.aggregateId());

		try {
			OrderReducedPayload payload = event.payloadAs(objectMapper, OrderReducedPayload.class);
			if (payload == null || payload.getOrderId() == null) {
				log.error("Invalid OrderReduced payload: eventId={}, payload={}", event.eventId(),
						event.payloadJson());
				throw new IllegalArgumentException("Invalid payload: orderId not found");
			}

			FreezeLookupRequest request = new FreezeLookupRequest();
			request.setRefType(RefType.ORDER);
			request.setRefId(String.valueOf(payload.getOrderId()));
			request.setAmount(payload.getAmount());

			vaultFreezeService.releaseFreeze(request);

			log.info("Freeze released for order: orderId={}, amount={}, eventId={}", payload.getOrderId(),
					payload.getAmount(), event.eventId());
		}
		catch (Exception e) {
			log.error("Failed to handle OrderReduced event in vault: eventId={}, aggregateId={}", event.eventId(),
					event.aggregateId(), e);
			throw new RuntimeException("Failed to process OrderReduced event in vault", e);
		}
	}

}
