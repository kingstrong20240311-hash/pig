package com.pig4cloud.pig.gym.service;

import com.pig4cloud.pig.gym.api.dto.CreateFmsAssessmentRequest;
import com.pig4cloud.pig.gym.api.dto.FmsAssessmentResult;

/**
 * FMS评估服务
 */
public interface FmsAssessmentService {

	FmsAssessmentResult createAssessment(CreateFmsAssessmentRequest request);

}
