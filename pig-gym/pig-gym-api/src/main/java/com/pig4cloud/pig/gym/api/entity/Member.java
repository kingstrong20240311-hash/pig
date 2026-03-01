/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pig.gym.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 会员
 *
 * @author claude
 * @date 2026-02-25
 */
@Data
@Schema(description = "会员")
@TableName("gym_member")
@EqualsAndHashCode(callSuper = true)
public class Member extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属门店/场馆", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long deptId;

	@Schema(description = "所属教练", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long coachId;

	@Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@Schema(description = "头像地址")
	private String avatarUrl;

	@Schema(description = "联系手机号", requiredMode = Schema.RequiredMode.REQUIRED)
	private String mobile;

	@Schema(description = "性别（枚举编码）")
	private String gender;

	@Schema(description = "生日")
	private LocalDate birthday;

	@Schema(description = "身高（cm）")
	private BigDecimal heightCm;

	@Schema(description = "体重（kg）")
	private BigDecimal weightKg;

	@Schema(description = "伤病史")
	private String injuryHistory;

	@Schema(description = "医疗注意事项")
	private String medicalNotes;

	@Schema(description = "训练目标补充")
	private String goalNotes;

	@Schema(description = "是否可继续预约与训练", requiredMode = Schema.RequiredMode.REQUIRED)
	private Boolean enabled;

	@Schema(description = "FMS评估得分")
	private Integer fmsScore;

	@Schema(description = "上次FMS评估得分")
	private Integer lastFmsScore;

	@Schema(description = "最近训练时间")
	private Instant lastTrainingAt;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标识,1:已删除,0:正常")
	private String delFlag;

}
