package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 训练动作记录
 */
@Data
@TableName("gym_training_exercise_record")
@EqualsAndHashCode(callSuper = true)
public class TrainingExerciseRecord extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long sessionId;

	private String exerciseName;

	private BigDecimal weightKg;

	private Integer reps;

	private Integer sets;

	private Integer sortOrder;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
