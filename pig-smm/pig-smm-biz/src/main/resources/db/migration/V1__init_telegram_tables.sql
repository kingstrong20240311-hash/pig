-- Telegram群组批量消息管理系统数据库初始化脚本
-- Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS pig DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE pig;

-- 1. TelegramGroup (Telegram群组)
CREATE TABLE telegram_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_id VARCHAR(255) NOT NULL UNIQUE COMMENT 'Telegram群组ID',
    group_name VARCHAR(255) NOT NULL COMMENT '群组名称',
    member_count INT DEFAULT 0 COMMENT '群组成员数量',
    join_failure_count INT DEFAULT 0 COMMENT '加群失败次数',
    total_join_count INT DEFAULT 0 COMMENT '总加群尝试次数',
    send_failure_count INT DEFAULT 0 COMMENT '发送失败次数',
    total_send_count INT DEFAULT 0 COMMENT '总发送尝试次数',
    last_join_attempt DATETIME NULL COMMENT '最后加群尝试时间',
    last_send_attempt DATETIME NULL COMMENT '最后发送尝试时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_group_member_count (member_count DESC),
    INDEX idx_group_join_stats (total_join_count, join_failure_count),
    INDEX idx_group_send_stats (total_send_count, send_failure_count),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Telegram群组信息表';

-- 2. TelegramAccount (Telegram账号)
CREATE TABLE telegram_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    third_party_account_id BIGINT NULL COMMENT '第三方账户ID',
    nick_name VARCHAR(255) NOT NULL COMMENT '账号昵称（firstName + lastName）',
    username VARCHAR(255) NULL COMMENT 'TG平台用户名（唯一标识）',
    phone VARCHAR(50) NULL COMMENT '手机号',
    tg_id VARCHAR(50) NULL COMMENT 'TG ID',
    groups TEXT NULL COMMENT '已加入的群组ID列表，JSON格式',
    is_available TINYINT(1) DEFAULT 1 COMMENT '是否可用',
    is_busy TINYINT(1) DEFAULT 0 COMMENT '是否忙碌',
    last_check_time DATETIME NULL COMMENT '最后检查时间',
    status_reason VARCHAR(500) NULL COMMENT '状态变更原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account_status (is_available, is_busy),
    INDEX idx_last_check_time (last_check_time),
    INDEX idx_third_party_account_id (third_party_account_id),
    UNIQUE INDEX uk_tg_id (tg_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Telegram账号信息表';

-- 3. MessageTask (消息任务)
CREATE TABLE message_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    third_party_task_id VARCHAR(255) NULL COMMENT '第三方平台任务ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    message_content TEXT NOT NULL COMMENT '消息内容',
    account_id BIGINT NOT NULL COMMENT '执行账号ID',
    target_groups TEXT NULL COMMENT '目标群组ID列表，JSON格式',
    task_status VARCHAR(50) DEFAULT 'PENDING' COMMENT '任务状态：PENDING, RUNNING, COMPLETED, FAILED, PAUSED',
    success_count INT DEFAULT 0 COMMENT '成功发送群组数量',
    failure_count INT DEFAULT 0 COMMENT '失败发送群组数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time DATETIME NULL COMMENT '开始执行时间',
    end_time DATETIME NULL COMMENT '完成时间',
    created_by VARCHAR(255) NULL COMMENT '创建人',
    INDEX idx_message_task_status (task_status, create_time),
    INDEX idx_account_id (account_id),
    INDEX idx_third_party_task_id (third_party_task_id),
    CONSTRAINT fk_message_task_account FOREIGN KEY (account_id) REFERENCES telegram_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息发送任务表';

-- 4. JoinTask (加群任务)
CREATE TABLE join_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    third_party_task_id VARCHAR(255) NULL COMMENT '第三方平台任务ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    groups TEXT NOT NULL COMMENT '群组执行状态JSON数组',
    task_status VARCHAR(50) DEFAULT 'PENDING' COMMENT '任务状态：PENDING, RUNNING, COMPLETED, FAILED, PAUSED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time DATETIME NULL COMMENT '开始执行时间',
    end_time DATETIME NULL COMMENT '完成时间',
    created_by VARCHAR(255) NULL COMMENT '创建人',
    INDEX idx_join_task_status (task_status, create_time),
    INDEX idx_third_party_task_id (third_party_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加群任务表';

-- 5. ApiHealthStatus (API健康状态)
CREATE TABLE api_health_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    api_name VARCHAR(255) NOT NULL COMMENT 'API名称',
    is_available TINYINT(1) DEFAULT 1 COMMENT '是否可用',
    last_check_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后检查时间',
    consecutive_failures INT DEFAULT 0 COMMENT '连续失败次数',
    response_time BIGINT DEFAULT 0 COMMENT '响应时间（毫秒）',
    error_message TEXT NULL COMMENT '错误信息',
    INDEX idx_api_name (api_name),
    INDEX idx_last_check_time (last_check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API健康状态监控表';

-- 初始化数据

-- 插入API健康状态监控记录
INSERT INTO api_health_status (api_name, is_available, last_check_time, consecutive_failures, response_time) VALUES
('telegram_api', 1, NOW(), 0, 0);

-- 插入示例群组数据（可选）
INSERT INTO telegram_group (group_id, group_name, member_count) VALUES
('test_group_001', '测试群组1', 100),
('test_group_002', '测试群组2', 250);

-- 插入示例账号数据（可选）
INSERT INTO telegram_account (nick_name, username, is_available) VALUES
('测试账号1', 'test_account_001', 1),
('测试账号2', 'test_account_002', 1);

-- 创建视图：群组优先级分值计算
CREATE VIEW v_group_priority AS
SELECT
    id,
    group_id,
    group_name,
    member_count,
    join_failure_count,
    total_join_count,
    send_failure_count,
    total_send_count,
    CASE
        WHEN total_join_count > 0
        THEN (total_join_count - join_failure_count) / total_join_count
        ELSE 1.0
    END as join_success_rate,
    CASE
        WHEN total_send_count > 0
        THEN (total_send_count - send_failure_count) / total_send_count
        ELSE 1.0
    END as send_success_rate,
    (
        CASE
            WHEN total_join_count > 0
            THEN (total_join_count - join_failure_count) / total_join_count
            ELSE 1.0
        END * 0.4 +
        CASE
            WHEN total_send_count > 0
            THEN (total_send_count - send_failure_count) / total_send_count
            ELSE 1.0
        END * 0.4 +
        member_count * 0.0001
    ) as priority_score,
    create_time,
    update_time
FROM telegram_group
ORDER BY priority_score DESC;
