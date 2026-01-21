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

package com.pig4cloud.pig.common.error.example;

import com.pig4cloud.pig.common.error.annotation.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单错误处理器示例
 * <p>
 * 演示如何使用 @ErrorHandler 注解定义错误补偿处理器
 * </p>
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@Component
public class OrderErrorHandlerExample {

	/**
	 * 处理订单取消补偿
	 * <p>
	 * 使用 @ErrorHandler 注解标注处理方法，指定领域、key 和 payload 类型
	 * </p>
	 * @param event 订单取消事件
	 */
	@ErrorHandler(domain = "order", key = "onCancel", payloadClass = OrderCancelEvent.class)
	public void handleOrderCancel(OrderCancelEvent event) {
		log.info("Compensating order cancel: orderId={}, userId={}, reason={}", event.getOrderId(), event.getUserId(),
				event.getReason());

		// 实际业务逻辑：取消订单、退款等
		// orderService.cancelOrder(event.getOrderId());
		// refundService.refund(event.getOrderId());
	}

	/**
	 * 处理订单创建补偿（使用 JSON 字符串参数）
	 * <p>
	 * 如果不指定 payloadClass，则会传递 JSON 字符串
	 * </p>
	 * @param payloadJson 订单创建事件 JSON
	 */
	@ErrorHandler(domain = "order", key = "onCreate")
	public void handleOrderCreate(String payloadJson) {
		log.info("Compensating order create with JSON: {}", payloadJson);

		// 手动解析 JSON 并处理
		// ObjectMapper mapper = new ObjectMapper();
		// OrderCreateEvent event = mapper.readValue(payloadJson,
		// OrderCreateEvent.class);
	}

	/**
	 * 处理订单支付补偿（无参数）
	 * <p>
	 * 对于不需要 payload 的处理器，可以不定义参数
	 * </p>
	 */
	@ErrorHandler(domain = "order", key = "onPayment")
	public void handleOrderPayment() {
		log.info("Compensating order payment without payload");

		// 处理逻辑
	}

}
