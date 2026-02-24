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
import com.pig4cloud.pig.gym.api.entity.TrainingExerciseMedia;
import com.pig4cloud.pig.gym.service.TrainingExerciseMediaService;
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
 * 训练动作素材控制器
 *
 * @author claude
 * @date 2026-02-24
 */
@RestController
@AllArgsConstructor
@RequestMapping("${gym.api-prefix:}/trainingexercisemedia")
@Tag(description = "trainingexercisemedia", name = "训练动作素材管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class TrainingExerciseMediaController {

	private final TrainingExerciseMediaService trainingExerciseMediaService;

	/**
	 * 分页查询训练动作素材
	 * @param page 分页对象
	 * @param trainingExerciseMedia 查询条件
	 * @return 分页查询结果
	 */
	@GetMapping("/page")
	@Operation(description = "分页查询", summary = "分页查询训练动作素材")
	public R getPage(@ParameterObject Page page, @ParameterObject TrainingExerciseMedia trainingExerciseMedia) {
		LambdaQueryWrapper<TrainingExerciseMedia> wrapper = Wrappers.<TrainingExerciseMedia>lambdaQuery()
			.eq(trainingExerciseMedia.getSessionId() != null, TrainingExerciseMedia::getSessionId,
					trainingExerciseMedia.getSessionId())
			.eq(trainingExerciseMedia.getExerciseRecordId() != null, TrainingExerciseMedia::getExerciseRecordId,
					trainingExerciseMedia.getExerciseRecordId())
			.orderByDesc(TrainingExerciseMedia::getCreateTime);

		return R.ok(trainingExerciseMediaService.page(page, wrapper));
	}

	/**
	 * 通过id查询训练动作素材
	 * @param id 训练动作素材id
	 * @return 包含查询结果的响应对象
	 */
	@Operation(description = "通过id查询训练动作素材", summary = "通过id查询训练动作素材")
	@GetMapping("/details/{id}")
	public R getById(@PathVariable("id") Long id) {
		return R.ok(trainingExerciseMediaService.getById(id));
	}

	/**
	 * 新增训练动作素材
	 * @param trainingExerciseMedia 训练动作素材对象
	 * @return 操作结果
	 */
	@PostMapping
	@SysLog("新增训练动作素材")
	@Operation(description = "新增训练动作素材", summary = "新增训练动作素材")
	@HasPermission("gym_trainingexercisemedia_add")
	public R save(@RequestBody TrainingExerciseMedia trainingExerciseMedia) {
		return R.ok(trainingExerciseMediaService.save(trainingExerciseMedia));
	}

	/**
	 * 修改训练动作素材
	 * @param trainingExerciseMedia 训练动作素材对象
	 * @return 操作结果
	 */
	@PutMapping
	@SysLog("修改训练动作素材")
	@HasPermission("gym_trainingexercisemedia_edit")
	@Operation(description = "修改训练动作素材", summary = "修改训练动作素材")
	public R update(@RequestBody TrainingExerciseMedia trainingExerciseMedia) {
		return R.ok(trainingExerciseMediaService.updateById(trainingExerciseMedia));
	}

	/**
	 * 通过id数组删除训练动作素材
	 * @param ids 要删除的训练动作素材id数组
	 * @return 操作结果
	 */
	@DeleteMapping
	@SysLog("删除训练动作素材")
	@HasPermission("gym_trainingexercisemedia_del")
	@Operation(description = "删除训练动作素材", summary = "删除训练动作素材")
	public R removeById(@RequestBody Long[] ids) {
		return R.ok(trainingExerciseMediaService.removeBatchByIds(List.of(ids)));
	}

	/**
	 * 导出excel表格
	 * @param trainingExerciseMedia 查询条件
	 * @return 训练动作素材列表
	 */
	@ResponseExcel
	@GetMapping("/export")
	@HasPermission("gym_trainingexercisemedia_export")
	@Operation(description = "导出训练动作素材", summary = "导出训练动作素材")
	public List<TrainingExerciseMedia> export(@ParameterObject TrainingExerciseMedia trainingExerciseMedia) {
		LambdaQueryWrapper<TrainingExerciseMedia> wrapper = Wrappers.<TrainingExerciseMedia>lambdaQuery()
			.eq(trainingExerciseMedia.getSessionId() != null, TrainingExerciseMedia::getSessionId,
					trainingExerciseMedia.getSessionId())
			.eq(trainingExerciseMedia.getExerciseRecordId() != null, TrainingExerciseMedia::getExerciseRecordId,
					trainingExerciseMedia.getExerciseRecordId());

		return trainingExerciseMediaService.list(wrapper);
	}

}
