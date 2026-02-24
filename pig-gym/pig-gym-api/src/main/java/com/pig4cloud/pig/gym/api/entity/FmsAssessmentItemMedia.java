package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import com.pig4cloud.pig.gym.api.enums.MediaType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * FMS动作素材
 */
@Data
@TableName("gym_fms_assessment_item_media")
@EqualsAndHashCode(callSuper = true)
public class FmsAssessmentItemMedia extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long assessmentId;

	private Long assessmentItemId;

	private MediaType mediaType;

	private String angleCode;

	private String mediaUrl;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
