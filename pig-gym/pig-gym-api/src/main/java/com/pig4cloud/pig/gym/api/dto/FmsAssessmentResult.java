package com.pig4cloud.pig.gym.api.dto;

import lombok.Data;

/**
 * FMS评估结果摘要
 */
@Data
public class FmsAssessmentResult {

	private Long assessmentId;

	private Integer totalScore;

	private Integer restrictedMovementCount;

	private Boolean hasAsymmetry;

	private Boolean hasPainRisk;

}
