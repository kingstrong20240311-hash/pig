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

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.gym.api.dto.*;
import com.pig4cloud.pig.gym.api.entity.TrainingSession;
import com.pig4cloud.pig.gym.service.FmsAssessmentService;
import com.pig4cloud.pig.gym.service.TrainingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Gym 健身管理控制器
 *
 * @author pig4cloud
 * @date 2026-02-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${gym.api-prefix:}")
@Tag(description = "gym", name = "健身管理")
public class GymController {

	private final TrainingSessionService trainingSessionService;

	private final FmsAssessmentService fmsAssessmentService;

	@PostMapping("/session/create")
	@Operation(summary = "发起训练预约", description = "仅教练发起训练预约")
	public R<TrainingSession> createSession(@Valid @RequestBody CreateTrainingSessionRequest request) {
		return R.ok(trainingSessionService.createSession(request));
	}

	@PostMapping("/session/cancel")
	@Operation(summary = "取消训练预约", description = "仅课程教练允许取消")
	public R<TrainingSession> cancelSession(@Valid @RequestBody CancelTrainingSessionRequest request) {
		return R.ok(trainingSessionService.cancelSession(request.getSessionId(), request.getCoachId(),
				request.getCancelReason()));
	}

	@PostMapping("/session/complete")
	@Operation(summary = "结课并记录动作", description = "动作重量/次数/组数必须完整")
	public R<TrainingSession> completeSession(@Valid @RequestBody CompleteTrainingSessionRequest request) {
		return R.ok(trainingSessionService.completeSession(request));
	}

	@PostMapping("/fms/create")
	@Operation(summary = "创建FMS评估", description = "执行FMS评分强校验并返回汇总结果")
	public R<FmsAssessmentResult> createFmsAssessment(@Valid @RequestBody CreateFmsAssessmentRequest request) {
		return R.ok(fmsAssessmentService.createAssessment(request));
	}

}
