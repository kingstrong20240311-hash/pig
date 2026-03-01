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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.gym.api.entity.TrainingSession;
import com.pig4cloud.pig.gym.service.TrainingSessionService;
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
 * 训练课程控制器
 *
 * @author claude
 * @date 2026-02-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("${gym.api-prefix:}/trainingsession")
@Tag(description = "trainingsession", name = "训练课程管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class TrainingSessionController {

	private final TrainingSessionService trainingSessionService;

	/**
	 * 分页查询训练课程
	 * @param page 分页对象
	 * @param trainingSession 查询条件
	 * @return 分页查询结果
	 */
	@GetMapping("/page")
	@Operation(description = "分页查询", summary = "分页查询训练课程")
	public R getPage(@ParameterObject Page page, @ParameterObject TrainingSession trainingSession) {
		LambdaQueryWrapper<TrainingSession> wrapper = Wrappers.<TrainingSession>lambdaQuery()
			.eq(trainingSession.getMemberId() != null, TrainingSession::getMemberId, trainingSession.getMemberId())
			.eq(trainingSession.getCoachId() != null, TrainingSession::getCoachId, trainingSession.getCoachId())
			.eq(trainingSession.getLessonPlanId() != null, TrainingSession::getLessonPlanId,
					trainingSession.getLessonPlanId())
			.eq(trainingSession.getStatus() != null, TrainingSession::getStatus, trainingSession.getStatus())
			.ge(trainingSession.getScheduledAt() != null, TrainingSession::getScheduledAt,
					trainingSession.getScheduledAt())
			.orderByDesc(TrainingSession::getCreateTime);

		return R.ok(trainingSessionService.page(page, wrapper));
	}

	/**
	 * 通过id查询训练课程
	 * @param id 训练课程id
	 * @return 包含查询结果的响应对象
	 */
	@Operation(description = "通过id查询训练课程", summary = "通过id查询训练课程")
	@GetMapping("/details/{id}")
	public R getById(@PathVariable("id") Long id) {
		return R.ok(trainingSessionService.getById(id));
	}

	/**
	 * 新增训练课程
	 * @param trainingSession 训练课程对象
	 * @return 操作结果
	 */
	@PostMapping
	@SysLog("新增训练课程")
	@Operation(description = "新增训练课程", summary = "新增训练课程")
	@HasPermission("gym_trainingsession_add")
	public R save(@RequestBody TrainingSession trainingSession) {
		return R.ok(trainingSessionService.save(trainingSession));
	}

	/**
	 * 修改训练课程
	 * @param trainingSession 训练课程对象
	 * @return 操作结果
	 */
	@PutMapping
	@SysLog("修改训练课程")
	@HasPermission("gym_trainingsession_edit")
	@Operation(description = "修改训练课程", summary = "修改训练课程")
	public R update(@RequestBody TrainingSession trainingSession) {
		return R.ok(trainingSessionService.updateById(trainingSession));
	}

	/**
	 * 通过id数组删除训练课程
	 * @param ids 要删除的训练课程id数组
	 * @return 操作结果
	 */
	@DeleteMapping
	@SysLog("删除训练课程")
	@HasPermission("gym_trainingsession_del")
	@Operation(description = "删除训练课程", summary = "删除训练课程")
	public R removeById(@RequestBody Long[] ids) {
		return R.ok(trainingSessionService.removeBatchByIds(List.of(ids)));
	}

	/**
	 * 查询教练当日排课列表
	 * @param startMs 查询起始时间（epoch millis，前端按本地时区计算当日 00:00）
	 * @param endMs 查询结束时间（epoch millis，前端按本地时区计算次日 00:00）
	 * @return 当日排课列表
	 */
	@GetMapping("/daily")
	@Operation(description = "查询当日排课", summary = "查询教练当日排课列表")
	public R getDailySchedule(@RequestParam Long startMs, @RequestParam Long endMs) {
		PigUser user = SecurityUtils.getUser();
		if (user == null) {
			return R.failed("无法获取当前用户信息");
		}
		return R.ok(trainingSessionService.getDailySchedule(user.getId(), startMs, endMs));
	}

	/**
	 * 导出excel表格
	 * @param trainingSession 查询条件
	 * @return 训练课程列表
	 */
	@ResponseExcel
	@GetMapping("/export")
	@HasPermission("gym_trainingsession_export")
	@Operation(description = "导出训练课程", summary = "导出训练课程")
	public List<TrainingSession> export(@ParameterObject TrainingSession trainingSession) {
		LambdaQueryWrapper<TrainingSession> wrapper = Wrappers.<TrainingSession>lambdaQuery()
			.eq(trainingSession.getMemberId() != null, TrainingSession::getMemberId, trainingSession.getMemberId())
			.eq(trainingSession.getCoachId() != null, TrainingSession::getCoachId, trainingSession.getCoachId())
			.eq(trainingSession.getStatus() != null, TrainingSession::getStatus, trainingSession.getStatus());

		return trainingSessionService.list(wrapper);
	}

}
