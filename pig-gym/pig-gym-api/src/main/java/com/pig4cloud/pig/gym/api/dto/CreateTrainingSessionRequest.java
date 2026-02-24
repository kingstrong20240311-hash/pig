package com.pig4cloud.pig.gym.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发起预约请求
 */
@Data
public class CreateTrainingSessionRequest {

	@NotNull
	private Long memberId;

	@NotNull
	private Long coachId;

	private Long lessonPlanId;

	@NotNull
	private LocalDateTime scheduledAt;

}
