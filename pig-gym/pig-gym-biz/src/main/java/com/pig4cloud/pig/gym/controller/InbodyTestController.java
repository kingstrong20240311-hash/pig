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

package com.pig4cloud.pig.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.gym.api.entity.InbodyTest;
import com.pig4cloud.pig.gym.service.InbodyTestService;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * InBody体测记录控制器
 *
 * @author claude
 * @date 2026-02-25
 */
@RestController
@AllArgsConstructor
@RequestMapping("${gym.api-prefix:}/inbody-test")
@Tag(description = "inbody-test", name = "InBody体测管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class InbodyTestController {

	private final InbodyTestService inbodyTestService;

	/**
	 * 分页查询InBody体测记录
	 * @param page 分页对象
	 * @param inbodyTest 查询条件
	 * @return 分页查询结果
	 */
	@GetMapping("/page")
	@Operation(description = "分页查询", summary = "分页查询InBody体测记录")
	public R getPage(@ParameterObject Page page, @ParameterObject InbodyTest inbodyTest) {
		LambdaQueryWrapper<InbodyTest> wrapper = Wrappers.<InbodyTest>lambdaQuery()
			.eq(inbodyTest.getMemberId() != null, InbodyTest::getMemberId, inbodyTest.getMemberId())
			.eq(inbodyTest.getCoachId() != null, InbodyTest::getCoachId, inbodyTest.getCoachId())
			.orderByDesc(InbodyTest::getTestDate);

		return R.ok(inbodyTestService.page(page, wrapper));
	}

	/**
	 * 通过id查询InBody体测记录
	 * @param id 记录id
	 * @return 包含查询结果的响应对象
	 */
	@Operation(description = "通过id查询InBody体测记录", summary = "通过id查询InBody体测记录")
	@GetMapping("/details/{id}")
	public R getById(@PathVariable("id") Long id) {
		return R.ok(inbodyTestService.getById(id));
	}

	/**
	 * 新增InBody体测记录
	 * @param inbodyTest 体测记录对象
	 * @return 操作结果
	 */
	@PostMapping
	@SysLog("新增InBody体测记录")
	@Operation(description = "新增InBody体测记录", summary = "新增InBody体测记录")
	@HasPermission("gym_inbody_test_add")
	public R save(@RequestBody InbodyTest inbodyTest) {
		return R.ok(inbodyTestService.save(inbodyTest));
	}

	/**
	 * 修改InBody体测记录
	 * @param inbodyTest 体测记录对象
	 * @return 操作结果
	 */
	@PutMapping
	@SysLog("修改InBody体测记录")
	@HasPermission("gym_inbody_test_edit")
	@Operation(description = "修改InBody体测记录", summary = "修改InBody体测记录")
	public R update(@RequestBody InbodyTest inbodyTest) {
		return R.ok(inbodyTestService.updateById(inbodyTest));
	}

	/**
	 * 通过id数组删除InBody体测记录
	 * @param ids 要删除的记录id数组
	 * @return 操作结果
	 */
	@DeleteMapping
	@SysLog("删除InBody体测记录")
	@HasPermission("gym_inbody_test_del")
	@Operation(description = "删除InBody体测记录", summary = "删除InBody体测记录")
	public R removeById(@RequestBody Long[] ids) {
		return R.ok(inbodyTestService.removeBatchByIds(List.of(ids)));
	}

	/**
	 * 导出excel表格
	 * @param inbodyTest 查询条件
	 * @return InBody体测记录列表
	 */
	@ResponseExcel
	@GetMapping("/export")
	@HasPermission("gym_inbody_test_export")
	@Operation(description = "导出InBody体测记录", summary = "导出InBody体测记录")
	public List<InbodyTest> export(@ParameterObject InbodyTest inbodyTest) {
		LambdaQueryWrapper<InbodyTest> wrapper = Wrappers.<InbodyTest>lambdaQuery()
			.eq(inbodyTest.getMemberId() != null, InbodyTest::getMemberId, inbodyTest.getMemberId())
			.eq(inbodyTest.getCoachId() != null, InbodyTest::getCoachId, inbodyTest.getCoachId())
			.orderByDesc(InbodyTest::getTestDate);

		return inbodyTestService.list(wrapper);
	}

}
