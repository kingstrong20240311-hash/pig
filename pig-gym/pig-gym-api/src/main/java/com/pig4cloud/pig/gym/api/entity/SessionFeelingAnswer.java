package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 训练后感受回答
 */
@Data
@TableName("gym_session_feeling_answer")
@EqualsAndHashCode(callSuper = true)
public class SessionFeelingAnswer extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long sessionId;

	private Long memberId;

	private Long coachId;

	private Long templateId;

	private String answerText;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
