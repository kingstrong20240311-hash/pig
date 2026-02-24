package com.pig4cloud.pig.gym.api.dto;

import com.pig4cloud.pig.gym.api.enums.FmsMovementType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * FMS动作评分入参
 */
@Data
public class FmsAssessmentItemInput {

	@NotNull
	private FmsMovementType movementType;

	private Integer leftScore;

	private Integer rightScore;

	private Integer finalScore;

	private Boolean hasClearingTest;

	private Boolean clearingTestPain;

	private String painPosition;

	private String compensationTags;

	private String remark;

}
