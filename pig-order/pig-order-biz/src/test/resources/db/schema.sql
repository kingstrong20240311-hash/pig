-- Test database schema for pig-order integration tests
-- Based on /db/pig_order.sql

CREATE TABLE IF NOT EXISTS ord_market (
  market_id   BIGINT NOT NULL,
  name        VARCHAR(128) NOT NULL,
  status      TINYINT NOT NULL,
  expire_at   TIMESTAMP NULL,

  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by   VARCHAR(64) NULL,
  update_by   VARCHAR(64) NULL,
  del_flag    CHAR(1) NOT NULL DEFAULT '0',

  PRIMARY KEY (market_id),
  KEY idx_market_status_expire (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ord_order (
  order_id           BIGINT NOT NULL,
  user_id            BIGINT NOT NULL,
  market_id          BIGINT NOT NULL,

  side               TINYINT NOT NULL,
  order_type         TINYINT NOT NULL,
  price              DECIMAL(36,18) NULL,
  quantity           DECIMAL(36,18) NOT NULL,
  remaining_quantity DECIMAL(36,18) NOT NULL,

  status             TINYINT NOT NULL,
  time_in_force      TINYINT NOT NULL DEFAULT 1,
  expire_at          TIMESTAMP NULL,

  reject_reason      VARCHAR(255) NULL,

  idempotency_key    VARCHAR(128) NOT NULL,
  version            INT NOT NULL DEFAULT 0,

  create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  create_by          VARCHAR(64) NULL,
  update_by          VARCHAR(64) NULL,
  del_flag           CHAR(1) NOT NULL DEFAULT '0',

  PRIMARY KEY (order_id),
  UNIQUE KEY uk_order_idem (idempotency_key),
  KEY idx_order_user_time (user_id, create_time),
  KEY idx_order_market_status (market_id, status, price, create_time),
  KEY idx_order_status (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ord_order_fill (
  trade_id         BIGINT NOT NULL,
  match_id         VARCHAR(64) NOT NULL,

  taker_order_id   BIGINT NOT NULL,
  maker_order_id   BIGINT NOT NULL,

  price            DECIMAL(36,18) NOT NULL,
  quantity         DECIMAL(36,18) NOT NULL,

  fee              DECIMAL(36,18) NULL,

  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by        VARCHAR(64) NULL,

  PRIMARY KEY (trade_id),
  UNIQUE KEY uk_fill_idem (match_id, taker_order_id, maker_order_id, trade_id),
  KEY idx_fill_taker (taker_order_id, create_time),
  KEY idx_fill_maker (maker_order_id, create_time),
  KEY idx_fill_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ord_order_cancel (
  cancel_id        BIGINT NOT NULL,
  order_id         BIGINT NOT NULL,
  reason           VARCHAR(255) NOT NULL,
  idempotency_key  VARCHAR(128) NOT NULL,

  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by        VARCHAR(64) NULL,

  PRIMARY KEY (cancel_id),
  UNIQUE KEY uk_cancel_idem (idempotency_key),
  KEY idx_cancel_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS outbox_event (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id         VARCHAR(64)  NOT NULL,
    domain           VARCHAR(32)  NOT NULL,
    aggregate_type   VARCHAR(64)  NOT NULL,
    aggregate_id     VARCHAR(64)  NOT NULL,
    event_type       VARCHAR(128) NOT NULL,
    payload_json     JSON         NOT NULL,
    headers_json     JSON         NULL,
    partition_key    VARCHAR(128) NOT NULL,

    status           TINYINT      NOT NULL DEFAULT 0,
    attempts         INT          NOT NULL DEFAULT 0,
    next_retry_time  DATETIME(3)  NULL,

    locked_by        VARCHAR(64)  NULL,
    locked_at        DATETIME(3)  NULL,

    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    UNIQUE KEY uk_event_id (event_id),
    KEY idx_pick (status, next_retry_time, locked_at),
    KEY idx_agg (aggregate_type, aggregate_id, id),
    KEY idx_domain (domain, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO ord_market (market_id, name, status, expire_at, del_flag)
VALUES (1, 'Test Market', 1, NULL, '0')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), expire_at = VALUES(expire_at);
