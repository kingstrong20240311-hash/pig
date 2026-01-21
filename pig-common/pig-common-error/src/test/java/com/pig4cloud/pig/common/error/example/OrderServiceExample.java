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

import com.pig4cloud.pig.common.error.service.ErrorRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务示例
 * <p>
 * 演示如何在业务代码中捕获异常并记录错误
 * </p>
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceExample {

	private final ErrorRecordService errorRecordService;

	/**
	 * 处理订单取消
	 * <p>
	 * 示例：在业务逻辑中捕获异常，并使用 ErrorRecordService 记录错误
	 * </p>
	 * @param event 订单取消事件
	 */
	public void processOrderCancel(OrderCancelEvent event) {
		try {
			// 业务逻辑：调用第三方服务、更新数据库等
			callExternalService(event);
			updateDatabase(event);
		}
		catch (Exception e) {
			log.error("Failed to process order cancel: orderId={}", event.getOrderId(), e);

			// 记录错误，等待后续补偿
			// 方式1：传递对象，自动序列化为 JSON
			errorRecordService.record("order", // 领域
					"order:onCancel", // 处理器 key
					event, // 原始数据对象
					e // 异常
			);

			// 可以选择抛出异常或返回失败
			// throw new RuntimeException("Order cancel failed", e);
		}
	}

	/**
	 * 处理订单创建（使用 JSON 字符串）
	 * @param orderId 订单ID
	 * @param orderJson 订单 JSON
	 */
	public void processOrderCreate(Long orderId, String orderJson) {
		try {
			// 业务逻辑
			doSomething(orderId);
		}
		catch (Exception e) {
			log.error("Failed to process order create: orderId={}", orderId, e);

			// 方式2：直接传递 JSON 字符串
			errorRecordService.record("order", // 领域
					"order:onCreate", // 处理器 key
					orderJson, // JSON 字符串
					"com.example.OrderCreateEvent", // 类名（可选）
					e // 异常
			);
		}
	}

	/**
	 * 批量处理订单（手动指定错误信息）
	 * @param events 订单事件列表
	 */
	public void processBatchOrders(java.util.List<OrderCancelEvent> events) {
		for (OrderCancelEvent event : events) {
			try {
				doSomething(event.getOrderId());
			}
			catch (Exception e) {
				log.error("Batch order processing failed: orderId={}", event.getOrderId(), e);

				// 方式3：手动指定错误消息和堆栈
				errorRecordService.record("order", "order:onCancel", event, e);
			}
		}
	}

	// 模拟业务方法
	private void callExternalService(OrderCancelEvent event) {
		// 调用外部服务
	}

	private void updateDatabase(OrderCancelEvent event) {
		// 更新数据库
	}

	private void doSomething(Long orderId) {
		// 业务逻辑
	}

}
