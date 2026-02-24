package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 备课动作项
 */
@Data
@TableName("gym_lesson_plan_exercise")
@EqualsAndHashCode(callSuper = true)
public class LessonPlanExercise extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long lessonPlanId;

	private String exerciseName;

	private String targetWeightRange;

	private String targetRepsRange;

	private String targetSetsRange;

	private Integer sortOrder;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
