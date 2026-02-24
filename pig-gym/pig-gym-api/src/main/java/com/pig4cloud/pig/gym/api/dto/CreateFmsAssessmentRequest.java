package com.pig4cloud.pig.gym.api.dto;

import com.pig4cloud.pig.gym.api.enums.AssessmentType;
import com.pig4cloud.pig.gym.api.enums.FmsVersionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建FMS评估请求
 */
@Data
public class CreateFmsAssessmentRequest {

	@NotNull
	private Long memberId;

	@NotNull
	private Long coachId;

	@NotNull
	private AssessmentType assessmentType;

	@NotNull
	private FmsVersionType versionType;

	private String trainingSuggestion;

	@Valid
	@NotEmpty
	private List<FmsAssessmentItemInput> items;

}
