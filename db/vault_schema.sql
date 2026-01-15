-- Vault Module Database Schema
-- Author: luka
-- Date: 2025-01-14

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
    decimals INT NOT NULL COMMENT 'Decimal places',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Is active',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vault Asset Table';

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
