package com.pig4cloud.pig.gym.api.vo;

import com.pig4cloud.pig.gym.api.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * 当日排课视图
 */
@Data
@Schema(description = "当日排课视图")
public class DailySessionVO {

	@Schema(description = "课程ID")
	private Long id;

	@Schema(description = "会员ID")
	private Long memberId;

	@Schema(description = "会员姓名")
	private String memberName;

	@Schema(description = "会员头像")
	private String memberAvatar;

	@Schema(description = "训练目标（来自备课）")
	private String trainingGoal;

	@Schema(description = "预约时间（epoch millis）")
	private Instant scheduledAt;

	@Schema(description = "课程状态")
	private SessionStatus status;

}
