-- Test Data Initialization for Vault Module
-- Author: luka
-- Date: 2025-01-15

-- Clean up existing test data
DELETE FROM vault_ledger_entry WHERE account_id = 1001;
DELETE FROM vault_freeze WHERE account_id = 1001;
DELETE FROM vault_balance WHERE account_id = 1001;
DELETE FROM vault_account WHERE account_id = 1001;
DELETE FROM vault_asset WHERE asset_id = 1;

-- 1. Insert test account
INSERT INTO vault_account (account_id, user_id, account_type, status, create_time, update_time)
VALUES (1001, 10001, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Insert test asset (USDC)
INSERT INTO vault_asset (asset_id, symbol, currency_id, decimals, is_active, create_time)
VALUES (1, 'USDC', 1, 6, TRUE, CURRENT_TIMESTAMP);

-- 3. Insert test balance (100 USDC available, 0 frozen)
INSERT INTO vault_balance (balance_id, account_id, asset_id, available, frozen, version, update_time)
VALUES (1, 1001, 1, 100.000000, 0.000000, 0, CURRENT_TIMESTAMP);
