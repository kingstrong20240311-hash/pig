package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import com.pig4cloud.pig.gym.api.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 训练课程
 */
@Data
@Schema(description = "训练课程")
@TableName("gym_training_session")
@EqualsAndHashCode(callSuper = true)
public class TrainingSession extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "会员ID", required = true)
	private Long memberId;

	@Schema(description = "教练ID", required = true)
	private Long coachId;

	@Schema(description = "备课ID")
	private Long lessonPlanId;

	@Schema(description = "预约时间", required = true)
	private LocalDateTime scheduledAt;

	@Schema(description = "结课时间")
	private LocalDateTime completedAt;

	@Schema(description = "课程状态", required = true)
	private SessionStatus status;

	@Schema(description = "取消原因")
	private String cancelReason;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标识,1:已删除,0:正常")
	private String delFlag;

}
