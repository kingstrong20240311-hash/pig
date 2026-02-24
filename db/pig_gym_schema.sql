-- ================================================
-- Pig Gym Module Database Schema (FMS + 训练过程管理)
-- ================================================

DROP DATABASE IF EXISTS `pig_gym`;
CREATE DATABASE `pig_gym` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `pig_gym`;

-- 1) 训练课程主表
CREATE TABLE IF NOT EXISTS `gym_training_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `coach_id` BIGINT NOT NULL,
    `lesson_plan_id` BIGINT NULL,
    `scheduled_at` DATETIME NOT NULL,
    `completed_at` DATETIME NULL,
    `status` VARCHAR(32) NOT NULL,
    `cancel_reason` VARCHAR(500) NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_training_session_member` (`member_id`),
    KEY `idx_training_session_coach` (`coach_id`),
    KEY `idx_training_session_schedule` (`scheduled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练课程';

-- 2) 备课
CREATE TABLE IF NOT EXISTS `gym_lesson_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `coach_id` BIGINT NOT NULL,
    `training_goal` VARCHAR(200) NOT NULL,
    `intensity_range` VARCHAR(1000) NULL,
    `risk_notes` VARCHAR(1000) NULL,
    `alternative_exercise_notes` VARCHAR(1000) NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_lesson_plan_member` (`member_id`),
    KEY `idx_lesson_plan_coach` (`coach_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备课';

-- 3) 备课动作项
CREATE TABLE IF NOT EXISTS `gym_lesson_plan_exercise` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `lesson_plan_id` BIGINT NOT NULL,
    `exercise_name` VARCHAR(100) NOT NULL,
    `target_weight_range` VARCHAR(100) NULL,
    `target_reps_range` VARCHAR(100) NULL,
    `target_sets_range` VARCHAR(100) NULL,
    `sort_order` INT NOT NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_lesson_plan_exercise_plan` (`lesson_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备课动作项';

-- 4) 训练动作记录
CREATE TABLE IF NOT EXISTS `gym_training_exercise_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `exercise_name` VARCHAR(100) NOT NULL,
    `weight_kg` DECIMAL(8,2) NOT NULL,
    `reps` INT NOT NULL,
    `sets` INT NOT NULL,
    `sort_order` INT NOT NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_training_record_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练动作记录';

-- 5) 训练动作素材
CREATE TABLE IF NOT EXISTS `gym_training_exercise_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `exercise_record_id` BIGINT NOT NULL,
    `detail_url` VARCHAR(500) NOT NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_training_media_session` (`session_id`),
    KEY `idx_training_media_record` (`exercise_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练动作素材';

-- 6) 课后体态照
CREATE TABLE IF NOT EXISTS `gym_posture_photo` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `member_id` BIGINT NOT NULL,
    `photo_type` VARCHAR(32) NOT NULL,
    `photo_url` VARCHAR(500) NOT NULL,
    `remark` VARCHAR(200) NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_posture_photo_session` (`session_id`),
    KEY `idx_posture_photo_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课后体态照';

-- 7) 感受问题模板
CREATE TABLE IF NOT EXISTS `gym_feeling_question_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(100) NOT NULL,
    `question_text` VARCHAR(200) NOT NULL,
    `enabled` TINYINT NOT NULL DEFAULT 1,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feeling_question_template_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='感受问题模板';

-- 8) 训练后感受回答
CREATE TABLE IF NOT EXISTS `gym_session_feeling_answer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `member_id` BIGINT NOT NULL,
    `coach_id` BIGINT NOT NULL,
    `template_id` BIGINT NOT NULL,
    `answer_text` VARCHAR(500) NOT NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_feeling_answer_session` (`session_id`),
    KEY `idx_feeling_answer_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练后感受回答';

-- 9) 投诉
CREATE TABLE IF NOT EXISTS `gym_complaint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL,
    `coach_id` BIGINT NOT NULL,
    `member_id` BIGINT NULL,
    `anonymous` TINYINT NOT NULL DEFAULT 0,
    `visible_to_coach` TINYINT NOT NULL DEFAULT 0,
    `content` VARCHAR(1000) NOT NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_complaint_coach` (`coach_id`),
    KEY `idx_complaint_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉';

-- 10) FMS评估主表
CREATE TABLE IF NOT EXISTS `gym_fms_assessment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `member_id` BIGINT NOT NULL,
    `coach_id` BIGINT NOT NULL,
    `assessment_type` VARCHAR(32) NOT NULL,
    `version_type` VARCHAR(32) NOT NULL,
    `total_score` INT NOT NULL,
    `restricted_movement_count` INT NOT NULL,
    `has_asymmetry` TINYINT NOT NULL,
    `has_pain_risk` TINYINT NOT NULL,
    `training_suggestion` VARCHAR(1000) NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_fms_assessment_member` (`member_id`),
    KEY `idx_fms_assessment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FMS评估主表';

-- 11) FMS动作评估项
CREATE TABLE IF NOT EXISTS `gym_fms_assessment_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `assessment_id` BIGINT NOT NULL,
    `movement_type` VARCHAR(64) NOT NULL,
    `left_score` INT NULL,
    `right_score` INT NULL,
    `final_score` INT NOT NULL,
    `has_clearing_test` TINYINT NOT NULL DEFAULT 0,
    `clearing_test_pain` TINYINT NULL,
    `pain_position` VARCHAR(200) NULL,
    `compensation_tags` VARCHAR(500) NULL,
    `remark` VARCHAR(500) NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_fms_item_assessment` (`assessment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FMS动作评估项';

-- 12) FMS动作素材
CREATE TABLE IF NOT EXISTS `gym_fms_assessment_item_media` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `assessment_id` BIGINT NOT NULL,
    `assessment_item_id` BIGINT NOT NULL,
    `media_type` VARCHAR(32) NOT NULL,
    `angle_code` VARCHAR(100) NOT NULL,
    `media_url` VARCHAR(500) NOT NULL,
    `create_by` VARCHAR(64) NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_fms_media_assessment` (`assessment_id`),
    KEY `idx_fms_media_item` (`assessment_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FMS动作素材';

SELECT '✅ pig_gym 结构初始化完成（FMS + 训练过程管理）' AS status;
