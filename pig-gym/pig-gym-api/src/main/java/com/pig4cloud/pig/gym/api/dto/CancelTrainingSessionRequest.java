package com.pig4cloud.pig.gym.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消课程请求
 */
@Data
public class CancelTrainingSessionRequest {

	@NotNull
	private Long sessionId;

	@NotNull
	private Long coachId;

	private String cancelReason;

}
