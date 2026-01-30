/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pig4cloud.pig.order.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.outbox.api.annotation.DomainEventHandler;
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderReducedPayload;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.api.feign.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handle OrderReduced events in order domain - call vault.releaseFreeze to release
 * (partial or full) freeze for order.
 *
 * @author lengleng
 * @date 2026-01-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReducedEventHandler {

	private final VaultService vaultService;

	private final ObjectMapper objectMapper;

	/**
	 * Handle OrderReduced event - release freeze amount via vault. Payload amount is
	 * already in decimal (asset amount from reducedVolume/rejectedVolume); conversion
	 * from exchange-core scale (×100) is done by the publisher.
	 */
	@DomainEventHandler(domain = "order", eventType = "OrderReduced")
	public void handleOrderReduced(DomainEventEnvelope<?> event) {
		log.info("Handling OrderReduced event in order domain: eventId={}, aggregateId={}", event.eventId(),
				event.aggregateId());

		OrderReducedPayload payload = event.payloadAs(objectMapper, OrderReducedPayload.class);
		if (payload == null || payload.getOrderId() == null) {
			log.error("Invalid OrderReduced payload: eventId={}, payload={}", event.eventId(), event.payloadJson());
			throw new IllegalArgumentException("Invalid payload: orderId not found");
		}
		// amount 为资产数量（已换算为 decimal），直接传给 vault
		FreezeLookupRequest request = new FreezeLookupRequest();
		request.setRefType(RefType.ORDER);
		request.setRefId(String.valueOf(payload.getOrderId()));
		request.setAmount(payload.getAmount());

		vaultService.releaseFreeze(request);

		log.info("Release freeze requested for order: orderId={}, amount={}, eventId={}", payload.getOrderId(),
				payload.getAmount(), event.eventId());
	}

}
