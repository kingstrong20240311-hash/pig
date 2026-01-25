-- Test Database Schema for Vault Module
-- Author: luka
-- Date: 2025-01-23

-- Create vault_account table
CREATE TABLE IF NOT EXISTS vault_account
(
    account_id   BIGINT PRIMARY KEY COMMENT 'Account ID',
    user_id      BIGINT COMMENT 'User ID (nullable for system accounts)',
    account_type INT         NOT NULL COMMENT 'Account Type: 1=USER, 2=SYSTEM',
    status       INT         NOT NULL COMMENT 'Account Status: 1=ACTIVE, 2=FROZEN, 3=CLOSED',
    create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Vault Account';

-- Create vault_asset table
CREATE TABLE IF NOT EXISTS vault_asset
(
    asset_id    BIGINT PRIMARY KEY COMMENT 'Asset ID',
    symbol      VARCHAR(32) NOT NULL COMMENT 'Asset symbol',
    decimals    INT         NOT NULL COMMENT 'Asset decimals',
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE COMMENT 'Is active',
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    UNIQUE KEY uk_symbol (symbol)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Vault Asset';

-- Create vault_balance table
CREATE TABLE IF NOT EXISTS vault_balance
(
    balance_id  BIGINT PRIMARY KEY COMMENT 'Balance ID',
    account_id  BIGINT         NOT NULL COMMENT 'Account ID',
    asset_id    BIGINT         NOT NULL COMMENT 'Asset ID',
    available   DECIMAL(36, 6) NOT NULL DEFAULT 0.000000 COMMENT 'Available balance',
    frozen      DECIMAL(36, 6) NOT NULL DEFAULT 0.000000 COMMENT 'Frozen balance',
    version     BIGINT         NOT NULL DEFAULT 0 COMMENT 'Version for optimistic locking',
    update_time TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    UNIQUE KEY uk_account_asset (account_id, asset_id),
    INDEX idx_account_id (account_id),
    INDEX idx_asset_id (asset_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Vault Balance';

-- Create vault_freeze table
CREATE TABLE IF NOT EXISTS vault_freeze
(
    freeze_id   BIGINT PRIMARY KEY COMMENT 'Freeze ID',
    account_id  BIGINT         NOT NULL COMMENT 'Account ID',
    asset_id    BIGINT         NOT NULL COMMENT 'Asset ID',
    amount      DECIMAL(36, 6) NOT NULL COMMENT 'Freeze amount',
    status      INT            NOT NULL COMMENT 'Freeze Status: 1=HELD, 2=RELEASED, 3=CLAIMED, 4=CONSUMED, 5=CANCELED, 6=EXPIRED',
    ref_type    INT            NOT NULL COMMENT 'Reference Type: 1=ORDER, 2=SETTLEMENT, 3=DEPOSIT',
    ref_id      VARCHAR(128)   NOT NULL COMMENT 'Reference ID',
    claim_time  TIMESTAMP NULL COMMENT 'Claim time',
    version     BIGINT         NOT NULL DEFAULT 0 COMMENT 'Version for optimistic locking',
    create_time TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    UNIQUE KEY uk_ref (ref_type, ref_id),
    INDEX idx_account_id (account_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Vault Freeze';

-- Create vault_ledger_entry table
CREATE TABLE IF NOT EXISTS vault_ledger_entry
(
    entry_id         BIGINT PRIMARY KEY COMMENT 'Entry ID',
    account_id       BIGINT         NOT NULL COMMENT 'Account ID',
    asset_id         BIGINT         NOT NULL COMMENT 'Asset ID',
    entry_type       INT            NOT NULL COMMENT 'Entry Type: 1=FREEZE, 2=UNFREEZE, 3=CLAIM, 4=CONSUME, 5=DEPOSIT',
    direction        INT            NOT NULL COMMENT 'Direction: 1=DEBIT, 2=CREDIT',
    amount           DECIMAL(36, 6) NOT NULL COMMENT 'Amount',
    idempotency_key  VARCHAR(255)   NOT NULL COMMENT 'Idempotency key',
    ref_type         INT            NOT NULL COMMENT 'Reference Type',
    ref_id           VARCHAR(128)   NOT NULL COMMENT 'Reference ID',
    before_available DECIMAL(36, 6) COMMENT 'Available balance before',
    before_frozen    DECIMAL(36, 6) COMMENT 'Frozen balance before',
    after_available  DECIMAL(36, 6) COMMENT 'Available balance after',
    after_frozen     DECIMAL(36, 6) COMMENT 'Frozen balance after',
    create_time      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    UNIQUE KEY uk_idempotency (idempotency_key),
    INDEX idx_account_id (account_id),
    INDEX idx_ref (ref_type, ref_id),
    INDEX idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Vault Ledger Entry';
