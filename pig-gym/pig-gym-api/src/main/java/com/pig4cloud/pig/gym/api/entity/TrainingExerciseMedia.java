package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 训练动作素材
 */
@Data
@TableName("gym_training_exercise_media")
@EqualsAndHashCode(callSuper = true)
public class TrainingExerciseMedia extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long sessionId;

	private Long exerciseRecordId;

	private String detailUrl;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
