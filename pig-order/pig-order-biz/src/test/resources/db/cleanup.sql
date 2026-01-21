-- Cleanup test data after each test
DELETE FROM ord_order_fill;
DELETE FROM ord_order_cancel;
DELETE FROM ord_order;
DELETE FROM ord_market;
DELETE FROM outbox_event;
