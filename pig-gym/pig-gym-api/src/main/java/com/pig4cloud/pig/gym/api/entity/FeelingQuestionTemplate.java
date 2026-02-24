package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 感受问题模板
 */
@Data
@TableName("gym_feeling_question_template")
@EqualsAndHashCode(callSuper = true)
public class FeelingQuestionTemplate extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private String code;

	private String questionText;

	private Boolean enabled;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
