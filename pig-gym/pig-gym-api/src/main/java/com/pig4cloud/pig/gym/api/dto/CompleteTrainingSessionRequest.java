package com.pig4cloud.pig.gym.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 结课请求
 */
@Data
public class CompleteTrainingSessionRequest {

	@NotNull
	private Long sessionId;

	@NotNull
	private Long coachId;

	private LocalDateTime completedAt;

	@Valid
	@NotEmpty
	private List<TrainingExerciseRecordInput> exerciseRecords;

}
