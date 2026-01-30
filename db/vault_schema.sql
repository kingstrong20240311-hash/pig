-- Vault Module Database Schema
-- Author: luka
-- Date: 2025-01-14

-- Create database
DROP DATABASE IF EXISTS `pig_vault`;
CREATE DATABASE `pig_vault` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- Use database
USE `pig_vault`;

-- ==========================
-- 1. vault_account table
-- ==========================
CREATE TABLE IF NOT EXISTS vault_account (
    account_id BIGINT PRIMARY KEY COMMENT 'Account ID',
    user_id BIGINT NULL COMMENT 'User ID (nullable for system accounts)',
    account_type TINYINT NOT NULL COMMENT '1=USER, 2=SYSTEM_FEE, 3=SYSTEM_TREASURY, 4=SYSTEM_INSURANCE',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=ACTIVE, 2=FROZEN, 3=CLOSED',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vault Account Table';

-- ==========================
-- 2. vault_asset table
-- ==========================
CREATE TABLE IF NOT EXISTS vault_asset (
    asset_id BIGINT PRIMARY KEY COMMENT 'Asset ID',
    symbol VARCHAR(32) NOT NULL UNIQUE COMMENT 'Asset symbol (e.g., USDC, ETH)',
    currency_id INT NULL COMMENT 'Currency ID for exchange-core',
    decimals INT NOT NULL COMMENT 'Decimal places',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Is active',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vault Asset Table';

CREATE UNIQUE INDEX uk_vault_asset_currency_id ON vault_asset (currency_id);

CREATE TABLE IF NOT EXISTS currency_id_seq (
  id INT PRIMARY KEY AUTO_INCREMENT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Currency ID sequence';

-- ==========================
-- 3. vault_balance table
-- ==========================
CREATE TABLE IF NOT EXISTS vault_balance (
    balance_id BIGINT PRIMARY KEY COMMENT 'Balance ID',
    account_id BIGINT NOT NULL COMMENT 'Account ID',
    asset_id BIGINT NOT NULL COMMENT 'Asset ID',
    available DECIMAL(36, 18) NOT NULL DEFAULT 0 COMMENT 'Available balance',
    frozen DECIMAL(36, 18) NOT NULL DEFAULT 0 COMMENT 'Frozen balance',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version for optimistic locking',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    UNIQUE KEY uk_account_asset (account_id, asset_id),
    INDEX idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vault Balance Table';

-- ==========================
-- 4. vault_freeze table
-- ==========================
CREATE TABLE IF NOT EXISTS vault_freeze (
    freeze_id BIGINT PRIMARY KEY COMMENT 'Freeze ID',
    account_id BIGINT NOT NULL COMMENT 'Account ID',
    asset_id BIGINT NOT NULL COMMENT 'Asset ID',
    amount DECIMAL(36, 18) NOT NULL COMMENT 'Freeze amount',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=HELD, 2=CLAIMED, 3=RELEASED, 4=CONSUMED, 5=CANCELED, 6=EXPIRED',
    ref_type TINYINT NOT NULL COMMENT '1=ORDER, 2=SETTLEMENT, 3=DEPOSIT, 4=WITHDRAW, 5=TRANSFER, 6=ADJUSTMENT, 7=SYSTEM',
    ref_id VARCHAR(128) NOT NULL COMMENT 'Reference ID',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version for optimistic locking',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    claim_time TIMESTAMP NULL COMMENT 'Claim time',
    UNIQUE KEY uk_ref (ref_type, ref_id),
    INDEX idx_account_asset (account_id, asset_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vault Freeze Table';

-- ==========================
-- 5. vault_ledger_entry table
-- ==========================
CREATE TABLE IF NOT EXISTS vault_ledger_entry (
    entry_id BIGINT PRIMARY KEY COMMENT 'Entry ID',
    account_id BIGINT NOT NULL COMMENT 'Account ID',
    asset_id BIGINT NOT NULL COMMENT 'Asset ID',
    entry_type TINYINT NOT NULL COMMENT '1=FREEZE, 2=UNFREEZE, 3=CLAIM, 4=CONSUME, 5=DEPOSIT, etc.',
    direction TINYINT NOT NULL COMMENT '1=DEBIT, 2=CREDIT',
    amount DECIMAL(36, 18) NOT NULL COMMENT 'Amount',
    idempotency_key VARCHAR(256) NOT NULL COMMENT 'Idempotency key',
    ref_type TINYINT NOT NULL COMMENT 'Reference type',
    ref_id VARCHAR(128) NOT NULL COMMENT 'Reference ID',
    before_available DECIMAL(36, 18) NULL COMMENT 'Available balance before operation',
    before_frozen DECIMAL(36, 18) NULL COMMENT 'Frozen balance before operation',
    after_available DECIMAL(36, 18) NULL COMMENT 'Available balance after operation',
    after_frozen DECIMAL(36, 18) NULL COMMENT 'Frozen balance after operation',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time (immutable)',
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    INDEX idx_account_asset_time (account_id, asset_id, create_time),
    INDEX idx_ref (ref_type, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vault Ledger Entry Table (Immutable Audit Log)';

-- ==========================
-- 6. outbox_event table
-- ==========================
DROP TABLE IF EXISTS `outbox_event`;
CREATE TABLE `outbox_event` (
    `id`               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `event_id`         VARCHAR(64)  NOT NULL COMMENT '全局唯一事件ID',
    `domain`           VARCHAR(32)  NOT NULL COMMENT '领域（order/vault/settlement）',
    `aggregate_type`   VARCHAR(64)  NOT NULL COMMENT '聚合类型',
    `aggregate_id`     VARCHAR(64)  NOT NULL COMMENT '聚合ID（业务主键）',
    `event_type`       VARCHAR(128) NOT NULL COMMENT '事件类型',
    `payload_json`     JSON         NOT NULL COMMENT '负载JSON',
    `headers_json`     JSON         NULL COMMENT '头部信息JSON',
    `partition_key`    VARCHAR(128) NOT NULL COMMENT '分区键（用于Kafka key，通常=aggregate_id）',

    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-NEW, 1-SENDING, 2-SENT, 3-RETRY, 9-DEAD',
    `attempts`         INT          NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `next_retry_time`  DATETIME(3)  NULL COMMENT '下次重试时间',

    `locked_by`        VARCHAR(64)  NULL COMMENT '锁定者标识',
    `locked_at`        DATETIME(3)  NULL COMMENT '锁定时间',

    `created_at`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_pick` (`status`, `next_retry_time`, `locked_at`),
    KEY `idx_agg` (`aggregate_type`, `aggregate_id`, `id`),
    KEY `idx_domain` (`domain`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Outbox事件表';

-- ==========================
-- 7. error_record table
-- ==========================
CREATE TABLE IF NOT EXISTS error_record (
  id BIGINT(20) NOT NULL COMMENT '主键ID',
  error_id VARCHAR(64) NOT NULL COMMENT '业务唯一ID',
  domain VARCHAR(64) NOT NULL COMMENT '领域',
  handler_key VARCHAR(255) NOT NULL COMMENT '处理函数标识',
  payload_json TEXT COMMENT '原始数据JSON',
  payload_class VARCHAR(255) COMMENT '强类型反序列化类名',
  status VARCHAR(32) NOT NULL COMMENT '状态：NEW/RETRYING/RESOLVED/DEAD',
  attempts INT(11) NOT NULL DEFAULT 0 COMMENT '尝试次数',
  next_retry_time TIMESTAMP(3) NULL COMMENT '下次重试时间',
  error_message VARCHAR(500) COMMENT '错误摘要',
  stack_trace TEXT COMMENT '堆栈信息',
  last_error_at TIMESTAMP(3) NULL COMMENT '最后一次错误时间',
  tags VARCHAR(500) COMMENT '扩展标签（JSON格式）',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_error_id (error_id),
  KEY idx_domain (domain),
  KEY idx_handler_key (handler_key),
  KEY idx_status (status),
  KEY idx_next_retry_time (next_retry_time),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误记录表';

-- ==========================
-- Initial Data
-- ==========================

-- Create vault account for admin user (user_id=1 from pig.sys_user)
INSERT INTO vault_account (account_id, user_id, account_type, status, create_time, update_time)
VALUES (1000000, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert USDC asset (currency_id=100, decimals=6 for standard stablecoin precision)
INSERT INTO vault_asset (asset_id, symbol, currency_id, decimals, is_active, create_time)
VALUES (1, 'USDC', 100, 6, TRUE, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE 
    currency_id = VALUES(currency_id),
    decimals = VALUES(decimals),
    is_active = VALUES(is_active);
