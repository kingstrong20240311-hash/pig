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

package com.pig4cloud.pig.order.service;

import com.pig4cloud.pig.order.api.dto.CancelOrderRequest;
import com.pig4cloud.pig.order.api.dto.CancelOrderResponse;
import com.pig4cloud.pig.order.api.dto.CreateOrderRequest;
import com.pig4cloud.pig.order.api.dto.CreateOrderResponse;

/**
 * Order Service Interface
 *
 * @author lengleng
 * @date 2025/01/17
 */
public interface OrderService {

	/**
	 * Create a new order with idempotency support
	 * @param request create order request
	 * @return create order response
	 */
	CreateOrderResponse createOrder(CreateOrderRequest request);

	/**
	 * Cancel an order (marks as CANCEL_REQUESTED, actual cancellation done by matching
	 * thread)
	 * @param request cancel order request
	 * @return cancel order response
	 */
	CancelOrderResponse cancelOrder(CancelOrderRequest request);

}
