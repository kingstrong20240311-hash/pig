-- ================================================
-- InBody体测记录表
-- ================================================
USE `pig`;

CREATE TABLE IF NOT EXISTS `gym_inbody_test` (
    `id`                        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id`                 BIGINT NOT NULL COMMENT '会员ID',
    `coach_id`                  BIGINT NULL COMMENT '教练ID',
    `test_date`                 DATETIME NOT NULL COMMENT '测试日期',

    -- 基础指标
    `height_cm`                 DECIMAL(5,1) NULL COMMENT '身高(cm)',
    `weight_kg`                 DECIMAL(5,2) NOT NULL COMMENT '体重(kg)',
    `bmi`                       DECIMAL(4,1) NULL COMMENT 'BMI体质指数',

    -- 体成分
    `body_fat_percentage`       DECIMAL(4,1) NULL COMMENT '体脂率(%)',
    `body_fat_mass_kg`          DECIMAL(5,2) NULL COMMENT '体脂量(kg)',
    `skeletal_muscle_mass_kg`   DECIMAL(5,2) NULL COMMENT '骨骼肌量(kg)',
    `lean_body_mass_kg`         DECIMAL(5,2) NULL COMMENT '去脂体重(kg)',

    -- 水分分析
    `total_body_water_kg`       DECIMAL(5,2) NULL COMMENT '体内水分总量(kg)',
    `intracellular_water_kg`    DECIMAL(5,2) NULL COMMENT '细胞内水分(kg)',
    `extracellular_water_kg`    DECIMAL(5,2) NULL COMMENT '细胞外水分(kg)',
    `ecw_tbw_ratio`             DECIMAL(4,3) NULL COMMENT '水肿指数(ECW/TBW)',

    -- 营养成分
    `protein_kg`                DECIMAL(5,2) NULL COMMENT '蛋白质(kg)',
    `minerals_kg`               DECIMAL(5,2) NULL COMMENT '无机盐(kg)',
    `bone_mineral_content_kg`   DECIMAL(5,2) NULL COMMENT '骨矿物质含量(kg)',

    -- 代谢与体型
    `basal_metabolic_rate_kcal` INT NULL COMMENT '基础代谢量(kcal)',
    `visceral_fat_level`        INT NULL COMMENT '内脏脂肪等级',
    `waist_hip_ratio`           DECIMAL(4,2) NULL COMMENT '腰臀比',

    -- 节段去脂体重
    `lean_mass_right_arm_kg`    DECIMAL(5,2) NULL COMMENT '右手臂去脂体重(kg)',
    `lean_mass_left_arm_kg`     DECIMAL(5,2) NULL COMMENT '左手臂去脂体重(kg)',
    `lean_mass_trunk_kg`        DECIMAL(5,2) NULL COMMENT '躯干去脂体重(kg)',
    `lean_mass_right_leg_kg`    DECIMAL(5,2) NULL COMMENT '右腿去脂体重(kg)',
    `lean_mass_left_leg_kg`     DECIMAL(5,2) NULL COMMENT '左腿去脂体重(kg)',

    -- 节段体脂
    `fat_mass_right_arm_kg`     DECIMAL(5,2) NULL COMMENT '右手臂体脂(kg)',
    `fat_mass_left_arm_kg`      DECIMAL(5,2) NULL COMMENT '左手臂体脂(kg)',
    `fat_mass_trunk_kg`         DECIMAL(5,2) NULL COMMENT '躯干体脂(kg)',
    `fat_mass_right_leg_kg`     DECIMAL(5,2) NULL COMMENT '右腿体脂(kg)',
    `fat_mass_left_leg_kg`      DECIMAL(5,2) NULL COMMENT '左腿体脂(kg)',

    -- 备注
    `remark`                    VARCHAR(500) NULL COMMENT '备注',

    -- 审计字段
    `create_by`                 VARCHAR(64) NULL COMMENT '创建人',
    `create_time`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`                 VARCHAR(64) NULL COMMENT '更新人',
    `update_time`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `del_flag`                  TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识(0正常1删除)',

    PRIMARY KEY (`id`),
    KEY `idx_inbody_member`    (`member_id`),
    KEY `idx_inbody_test_date` (`test_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='InBody体测记录';

SELECT '✅ gym_inbody_test 创建完成' AS status;
