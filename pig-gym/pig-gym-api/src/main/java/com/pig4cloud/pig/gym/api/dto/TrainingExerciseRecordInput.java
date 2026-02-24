package com.pig4cloud.pig.gym.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 动作记录入参
 */
@Data
public class TrainingExerciseRecordInput {

	@NotBlank
	private String exerciseName;

	@NotNull
	private BigDecimal weightKg;

	@NotNull
	@Min(1)
	private Integer reps;

	@NotNull
	@Min(1)
	private Integer sets;

	@NotNull
	@Min(1)
	private Integer sortOrder;

}
