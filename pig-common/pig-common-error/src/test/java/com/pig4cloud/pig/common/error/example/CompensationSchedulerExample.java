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

import com.pig4cloud.pig.common.error.service.ErrorCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 补偿定时任务示例
 * <p>
 * 演示如何通过定时任务周期性执行批量补偿
 * </p>
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationSchedulerExample {

	private final ErrorCompensationService compensationService;

	/**
	 * 每分钟执行一次订单领域的错误补偿
	 */
	@Scheduled(fixedDelay = 60000) // 每60秒执行一次
	public void compensateOrderErrors() {
		log.info("Starting order error compensation...");

		try {
			// 批量补偿订单领域的错误，最多处理100条
			int successCount = compensationService.compensateBatch("order", 100);
			log.info("Order error compensation completed: successCount={}", successCount);
		}
		catch (Exception e) {
			log.error("Order error compensation failed", e);
		}
	}

	/**
	 * 每5分钟执行一次所有领域的错误补偿
	 */
	@Scheduled(fixedDelay = 300000) // 每300秒（5分钟）执行一次
	public void compensateAllErrors() {
		log.info("Starting all domain error compensation...");

		try {
			// 不指定领域，补偿所有领域的错误
			int successCount = compensationService.compensateBatch(null, 200);
			log.info("All domain error compensation completed: successCount={}", successCount);
		}
		catch (Exception e) {
			log.error("All domain error compensation failed", e);
		}
	}

	/**
	 * 特定时间执行补偿（例如每天凌晨2点）
	 */
	@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
	public void compensateErrorsAtNight() {
		log.info("Starting nightly error compensation...");

		// 针对不同领域分别执行补偿
		compensateDomain("order", 500);
		compensateDomain("vault", 500);
		compensateDomain("settlement", 500);
	}

	/**
	 * 补偿指定领域的错误
	 * @param domain 领域
	 * @param limit 限制数量
	 */
	private void compensateDomain(String domain, int limit) {
		try {
			int successCount = compensationService.compensateBatch(domain, limit);
			log.info("Domain {} compensation completed: successCount={}", domain, successCount);
		}
		catch (Exception e) {
			log.error("Domain {} compensation failed", domain, e);
		}
	}

}
