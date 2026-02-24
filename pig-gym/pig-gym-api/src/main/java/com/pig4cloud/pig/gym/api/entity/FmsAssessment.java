package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import com.pig4cloud.pig.gym.api.enums.AssessmentType;
import com.pig4cloud.pig.gym.api.enums.FmsVersionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * FMS评估主表
 */
@Data
@TableName("gym_fms_assessment")
@EqualsAndHashCode(callSuper = true)
public class FmsAssessment extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long memberId;

	private Long coachId;

	private AssessmentType assessmentType;

	private FmsVersionType versionType;

	private Integer totalScore;

	private Integer restrictedMovementCount;

	private Boolean hasAsymmetry;

	private Boolean hasPainRisk;

	private String trainingSuggestion;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
