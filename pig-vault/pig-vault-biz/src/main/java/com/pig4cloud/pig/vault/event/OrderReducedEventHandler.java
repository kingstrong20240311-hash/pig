/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.vault.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderReducedPayload;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handle OrderReduced events for vault freeze release.
 *
 * @author pig4cloud
 * @date 2026-01-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReducedEventHandler {

	private final VaultFreezeService vaultFreezeService;

	private final ObjectMapper objectMapper;

	public void handleOrderReduced(DomainEventEnvelope<?> event) {
		try {
			if (event == null || event.payload() == null) {
				throw new IllegalArgumentException("Invalid payload");
			}

			OrderReducedPayload payload = parsePayload(event.payload());
			if (payload.getOrderId() == null) {
				throw new IllegalArgumentException("Invalid payload: orderId not found");
			}

			FreezeLookupRequest request = new FreezeLookupRequest();
			request.setRefType(RefType.ORDER);
			request.setRefId(String.valueOf(payload.getOrderId()));
			request.setAmount(payload.getAmount());

			vaultFreezeService.releaseFreeze(request);
		}
		catch (Exception e) {
			log.error("Failed to process OrderReduced event in vault", e);
			throw new RuntimeException("Failed to process OrderReduced event in vault", e);
		}
	}

	private OrderReducedPayload parsePayload(Object payload) throws Exception {
		if (payload instanceof OrderReducedPayload orderReducedPayload) {
			return orderReducedPayload;
		}
		if (payload instanceof String payloadJson) {
			return objectMapper.readValue(payloadJson, OrderReducedPayload.class);
		}
		throw new IllegalArgumentException("Invalid payload type");
	}

}
