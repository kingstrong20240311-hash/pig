-- Test data for state recovery tests
-- This data is loaded BEFORE the application starts

-- Insert MATCHING status orders
INSERT INTO ord_order (
    order_id, user_id, market_id,
    side, order_type, price, quantity, remaining_quantity,
    status, time_in_force,
    idempotency_key, version, del_flag
) VALUES
(1000, 100, 1, 1, 1, 100.00, 10.00, 10.00, 3, 1, 'recovery-matching-1', 0, '0'),
(1001, 100, 1, 2, 1, 100.00, 5.00, 5.00, 3, 1, 'recovery-matching-2', 0, '0');

-- Insert OPEN status orders
INSERT INTO ord_order (
    order_id, user_id, market_id,
    side, order_type, price, quantity, remaining_quantity,
    status, time_in_force,
    idempotency_key, version, del_flag
) VALUES
(2000, 100, 1, 1, 1, 100.00, 10.00, 10.00, 2, 1, 'recovery-open-1', 0, '0');

-- Insert PARTIALLY_FILLED status orders
INSERT INTO ord_order (
    order_id, user_id, market_id,
    side, order_type, price, quantity, remaining_quantity,
    status, time_in_force,
    idempotency_key, version, del_flag
) VALUES
(2001, 100, 1, 2, 1, 100.00, 10.00, 7.00, 4, 1, 'recovery-partial-1', 0, '0');
