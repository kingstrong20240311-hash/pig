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

package com.pig4cloud.pig.common.error.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import com.pig4cloud.pig.common.error.enums.ErrorRecordStatus;
import com.pig4cloud.pig.common.error.mapper.ErrorRecordMapper;
import com.pig4cloud.pig.common.error.service.ErrorCompensationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.*;

/**
 * 错误记录控制器
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@RestController
@RequestMapping("/error/records")
@RequiredArgsConstructor
@Tag(description = "错误记录管理", name = "ErrorRecordController")
@ConditionalOnWebApplication
public class ErrorRecordController {

	private final ErrorCompensationService compensationService;

	private final ErrorRecordMapper errorRecordMapper;

	/**
	 * 单条补偿
	 * @param id 错误记录ID
	 * @return 补偿结果
	 */
	@PostMapping("/{id}/compensate")
	@Operation(summary = "单条补偿")
	public R<Boolean> compensate(@PathVariable Long id) {
		boolean success = compensationService.compensate(id);
		return success ? R.ok(true) : R.failed("补偿失败");
	}

	/**
	 * 批量补偿
	 * @param domain 领域（可选）
	 * @param limit 限制数量
	 * @return 补偿结果
	 */
	@PostMapping("/compensate-batch")
	@Operation(summary = "批量补偿")
	public R<Integer> compensateBatch(@RequestParam(required = false) String domain,
			@RequestParam(defaultValue = "100") int limit) {
		int successCount = compensationService.compensateBatch(domain, limit);
		return R.ok(successCount);
	}

	/**
	 * 分页查询错误记录
	 * @param page 页码
	 * @param size 每页大小
	 * @param domain 领域（可选）
	 * @param status 状态（可选）
	 * @return 分页结果
	 */
	@GetMapping("/page")
	@Operation(summary = "分页查询错误记录")
	public R<IPage<ErrorRecord>> page(@RequestParam(defaultValue = "1") long page,
			@RequestParam(defaultValue = "10") long size, @RequestParam(required = false) String domain,
			@RequestParam(required = false) ErrorRecordStatus status) {
		Page<ErrorRecord> pageParam = new Page<>(page, size);
		IPage<ErrorRecord> result = errorRecordMapper.selectPage(pageParam,
				Wrappers.<ErrorRecord>lambdaQuery()
					.eq(domain != null, ErrorRecord::getDomain, domain)
					.eq(status != null, ErrorRecord::getStatus, status)
					.orderByDesc(ErrorRecord::getCreatedAt));
		return R.ok(result);
	}

	/**
	 * 获取错误记录详情
	 * @param id 错误记录ID
	 * @return 错误记录
	 */
	@GetMapping("/{id}")
	@Operation(summary = "获取错误记录详情")
	public R<ErrorRecord> getById(@PathVariable Long id) {
		ErrorRecord record = errorRecordMapper.selectById(id);
		return record != null ? R.ok(record) : R.failed("记录不存在");
	}

}
