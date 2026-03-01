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

/**
 * InBody体测记录
 *
 * @author claude
 * @date 2026-02-25
 */
@Data
@Schema(description = "InBody体测记录")
@TableName("gym_inbody_test")
@EqualsAndHashCode(callSuper = true)
public class InbodyTest extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "会员ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long memberId;

	@Schema(description = "教练ID")
	private Long coachId;

	@Schema(description = "测试日期", requiredMode = Schema.RequiredMode.REQUIRED)
	private Instant testDate;

	// 基础指标
	@Schema(description = "身高(cm)")
	private BigDecimal heightCm;

	@Schema(description = "体重(kg)", requiredMode = Schema.RequiredMode.REQUIRED)
	private BigDecimal weightKg;

	@Schema(description = "BMI体质指数")
	private BigDecimal bmi;

	// 体成分
	@Schema(description = "体脂率(%)")
	private BigDecimal bodyFatPercentage;

	@Schema(description = "体脂量(kg)")
	private BigDecimal bodyFatMassKg;

	@Schema(description = "骨骼肌量(kg)")
	private BigDecimal skeletalMuscleMassKg;

	@Schema(description = "去脂体重(kg)")
	private BigDecimal leanBodyMassKg;

	// 水分分析
	@Schema(description = "体内水分总量(kg)")
	private BigDecimal totalBodyWaterKg;

	@Schema(description = "细胞内水分(kg)")
	private BigDecimal intracellularWaterKg;

	@Schema(description = "细胞外水分(kg)")
	private BigDecimal extracellularWaterKg;

	@Schema(description = "水肿指数(ECW/TBW)")
	private BigDecimal ecwTbwRatio;

	// 营养成分
	@Schema(description = "蛋白质(kg)")
	private BigDecimal proteinKg;

	@Schema(description = "无机盐(kg)")
	private BigDecimal mineralsKg;

	@Schema(description = "骨矿物质含量(kg)")
	private BigDecimal boneMineralContentKg;

	// 代谢与体型
	@Schema(description = "基础代谢量(kcal)")
	private Integer basalMetabolicRateKcal;

	@Schema(description = "内脏脂肪等级")
	private Integer visceralFatLevel;

	@Schema(description = "腰臀比")
	private BigDecimal waistHipRatio;

	// 节段去脂体重
	@Schema(description = "右手臂去脂体重(kg)")
	private BigDecimal leanMassRightArmKg;

	@Schema(description = "左手臂去脂体重(kg)")
	private BigDecimal leanMassLeftArmKg;

	@Schema(description = "躯干去脂体重(kg)")
	private BigDecimal leanMassTrunkKg;

	@Schema(description = "右腿去脂体重(kg)")
	private BigDecimal leanMassRightLegKg;

	@Schema(description = "左腿去脂体重(kg)")
	private BigDecimal leanMassLeftLegKg;

	// 节段体脂
	@Schema(description = "右手臂体脂(kg)")
	private BigDecimal fatMassRightArmKg;

	@Schema(description = "左手臂体脂(kg)")
	private BigDecimal fatMassLeftArmKg;

	@Schema(description = "躯干体脂(kg)")
	private BigDecimal fatMassTrunkKg;

	@Schema(description = "右腿体脂(kg)")
	private BigDecimal fatMassRightLegKg;

	@Schema(description = "左腿体脂(kg)")
	private BigDecimal fatMassLeftLegKg;

	@Schema(description = "备注")
	private String remark;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标识,1:已删除,0:正常")
	private String delFlag;

}
