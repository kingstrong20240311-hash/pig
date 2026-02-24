package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 备课
 */
@Data
@TableName("gym_lesson_plan")
@EqualsAndHashCode(callSuper = true)
public class LessonPlan extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long memberId;

	private Long coachId;

	@Schema(description = "训练目标")
	private String trainingGoal;

	@Schema(description = "强度范围")
	private String intensityRange;

	@Schema(description = "风险备注")
	private String riskNotes;

	@Schema(description = "替代动作备注")
	private String alternativeExerciseNotes;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
