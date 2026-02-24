package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import com.pig4cloud.pig.gym.api.enums.PosturePhotoType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 课后体态照片
 */
@Data
@TableName("gym_posture_photo")
@EqualsAndHashCode(callSuper = true)
public class PosturePhoto extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long sessionId;

	private Long memberId;

	private PosturePhotoType photoType;

	private String photoUrl;

	private String remark;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	private String delFlag;

}
