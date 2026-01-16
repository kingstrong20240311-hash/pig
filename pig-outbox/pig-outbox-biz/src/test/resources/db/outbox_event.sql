-- Outbox事件表
CREATE TABLE IF NOT EXISTS outbox_event
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    event_id         VARCHAR(64)  NOT NULL COMMENT '全局唯一事件ID',
    domain           VARCHAR(32)  NOT NULL COMMENT '领域（order/vault/settlement）',
    aggregate_type   VARCHAR(64)  NOT NULL COMMENT '聚合类型',
    aggregate_id     VARCHAR(64)  NOT NULL COMMENT '聚合ID（业务主键）',
    event_type       VARCHAR(128) NOT NULL COMMENT '事件类型',
    payload_json     JSON         NOT NULL COMMENT '负载JSON',
    headers_json     JSON         NULL COMMENT '头部信息JSON',
    partition_key    VARCHAR(128) NOT NULL COMMENT '分区键（用于Kafka key，通常=aggregate_id）',

    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-NEW, 1-SENDING, 2-SENT, 3-RETRY, 9-DEAD',
    attempts         INT          NOT NULL DEFAULT 0 COMMENT '尝试次数',
    next_retry_time  DATETIME(3)  NULL COMMENT '下次重试时间',

    locked_by        VARCHAR(64)  NULL COMMENT '锁定者标识',
    locked_at        DATETIME(3)  NULL COMMENT '锁定时间',

    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    UNIQUE KEY uk_event_id (event_id),
    KEY idx_pick (status, next_retry_time, locked_at),
    KEY idx_agg (aggregate_type, aggregate_id, id),
    KEY idx_domain (domain, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Outbox事件表';
