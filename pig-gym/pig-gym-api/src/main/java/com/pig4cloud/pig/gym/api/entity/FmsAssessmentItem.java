package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import com.pig4cloud.pig.gym.api.enums.FmsMovementType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * FMS评估动作项
 */
@Data
@TableName("gym_fms_assessment_item")
@EqualsAndHashCode(callSuper = true)
public class FmsAssessmentItem extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long assessmentId;

	private FmsMovementType movementType;

	private Integer leftScore;

	private Integer rightScore;

	private Integer finalScore;

	private Boolean hasClearingTest;

	private Boolean clearingTestPain;

	private String painPosition;

	private String compensationTags;

	private String remark;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
