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
import com.pig4cloud.pig.outbox.api.model.DomainEventEnvelope;
import com.pig4cloud.pig.outbox.api.payload.order.OrderReducedPayload;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeResponse;
import com.pig4cloud.pig.vault.api.enums.FreezeStatus;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.service.VaultFreezeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for OrderReducedEventHandler
 *
 * @author pig4cloud
 * @date 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
class OrderReducedEventHandlerTest {

	@Mock
	private VaultFreezeService vaultFreezeService;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private OrderReducedEventHandler orderReducedEventHandler;

	@Test
	void testHandleOrderReduced_Success() {
		Long orderId = 1001L;
		BigDecimal amount = new BigDecimal("50.00");
		OrderReducedPayload payload = new OrderReducedPayload(orderId, amount);

		DomainEventEnvelope<OrderReducedPayload> event = new DomainEventEnvelope<>("evt-1", "order", "Order",
				String.valueOf(orderId), "OrderReduced", System.currentTimeMillis(), null, payload);

		FreezeResponse response = new FreezeResponse();
		response.setFreezeId("1");
		response.setStatus(FreezeStatus.RELEASED);
		response.setAmount(BigDecimal.ZERO);
		when(vaultFreezeService.releaseFreeze(any(FreezeLookupRequest.class))).thenReturn(response);

		orderReducedEventHandler.handleOrderReduced(event);

		ArgumentCaptor<FreezeLookupRequest> requestCaptor = ArgumentCaptor.forClass(FreezeLookupRequest.class);
		verify(vaultFreezeService).releaseFreeze(requestCaptor.capture());

		FreezeLookupRequest request = requestCaptor.getValue();
		assertThat(request.getRefType()).isEqualTo(RefType.ORDER);
		assertThat(request.getRefId()).isEqualTo("1001");
		assertThat(request.getAmount()).isEqualByComparingTo(amount);
	}

	@Test
	void testHandleOrderReduced_InvalidPayload_Throws() {
		DomainEventEnvelope<?> event = new DomainEventEnvelope<>("evt-2", "order", "Order", "1002", "OrderReduced",
				System.currentTimeMillis(), null, null);

		assertThatThrownBy(() -> orderReducedEventHandler.handleOrderReduced(event))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderReduced event in vault");
	}

	@Test
	void testHandleOrderReduced_NullOrderId_Throws() {
		OrderReducedPayload payload = new OrderReducedPayload(null, new BigDecimal("10"));

		DomainEventEnvelope<?> event = new DomainEventEnvelope<>("evt-3", "order", "Order", "1003", "OrderReduced",
				System.currentTimeMillis(), null, payload);

		assertThatThrownBy(() -> orderReducedEventHandler.handleOrderReduced(event))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Failed to process OrderReduced event in vault");
	}

}
