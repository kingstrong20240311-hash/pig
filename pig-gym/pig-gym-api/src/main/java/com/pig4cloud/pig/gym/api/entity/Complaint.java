package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 投诉
 */
@Data
@TableName("gym_complaint")
@EqualsAndHashCode(callSuper = true)
public class Complaint extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long sessionId;

	private Long coachId;

	private Long memberId;

	private Boolean anonymous;

	private Boolean visibleToCoach;

	private String content;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
