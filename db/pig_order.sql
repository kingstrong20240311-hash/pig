/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- ----------------------------
-- Database: pig_order
-- Description: Order domain database schema
-- ----------------------------

-- ----------------------------
-- Table structure for ord_order (Order Aggregate Root)
-- ----------------------------
DROP TABLE IF EXISTS `ord_order`;
CREATE TABLE `ord_order` (
  `order_id`           BIGINT NOT NULL COMMENT '订单ID',
  `user_id`            BIGINT NOT NULL COMMENT '用户ID',
  `market_id`          BIGINT NOT NULL COMMENT '市场ID',

  `side`               TINYINT NOT NULL COMMENT '方向: 1=BUY, 2=SELL',
  `order_type`         TINYINT NOT NULL COMMENT '订单类型: 1=LIMIT, 2=MARKET',
  `price`              DECIMAL(36,18) NULL COMMENT '价格 (LIMIT订单必填)',
  `quantity`           DECIMAL(36,18) NOT NULL COMMENT '原始数量',
  `remaining_quantity` DECIMAL(36,18) NOT NULL COMMENT '剩余可成交数量',

  `status`             TINYINT NOT NULL COMMENT '状态: 1=CREATED, 2=OPEN, 3=MATCHING, 4=PARTIALLY_FILLED, 5=FILLED, 6=CANCEL_REQUESTED, 7=CANCELLED, 8=EXPIRED, 9=REJECTED',
  `time_in_force`      TINYINT NOT NULL DEFAULT 1 COMMENT '有效期: 1=GTC, 2=IOC, 3=FOK, 4=GTD',
  `expire_at`          TIMESTAMP NULL COMMENT '过期时间 (GTD订单使用)',

  `reject_reason`      VARCHAR(255) NULL COMMENT '拒绝原因',

  `idempotency_key`    VARCHAR(128) NOT NULL COMMENT '幂等键',
  `version`            INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',

  `create_time`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`          VARCHAR(64) NULL COMMENT '创建人',
  `update_by`          VARCHAR(64) NULL COMMENT '更新人',
  `del_flag`           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志: 0=正常, 1=删除',

  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_idem` (`idempotency_key`),
  KEY `idx_order_user_time` (`user_id`, `create_time`),
  KEY `idx_order_market_status` (`market_id`, `status`, `price`, `create_time`),
  KEY `idx_order_status` (`status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- Add constraints (if MySQL version supports CHECK constraints)
-- ALTER TABLE ord_order ADD CONSTRAINT chk_remaining_qty CHECK (remaining_quantity >= 0);
-- ALTER TABLE ord_order ADD CONSTRAINT chk_quantity CHECK (quantity > 0);

-- ----------------------------
-- Table structure for ord_order_fill (Trade/Fill Details)
-- ----------------------------
DROP TABLE IF EXISTS `ord_order_fill`;
CREATE TABLE `ord_order_fill` (
  `trade_id`         BIGINT NOT NULL COMMENT '成交ID',
  `match_id`         VARCHAR(64) NOT NULL COMMENT '撮合ID (幂等主键)',

  `taker_order_id`   BIGINT NOT NULL COMMENT 'Taker订单ID',
  `maker_order_id`   BIGINT NOT NULL COMMENT 'Maker订单ID',

  `price`            DECIMAL(36,18) NOT NULL COMMENT '成交价格',
  `quantity`         DECIMAL(36,18) NOT NULL COMMENT '成交数量',

  `fee`              DECIMAL(36,18) NULL COMMENT '手续费',

  `create_time`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64) NULL COMMENT '创建人',

  PRIMARY KEY (`trade_id`),
  UNIQUE KEY `uk_fill_idem` (`match_id`, `taker_order_id`, `maker_order_id`, `trade_id`),
  KEY `idx_fill_taker` (`taker_order_id`, `create_time`),
  KEY `idx_fill_maker` (`maker_order_id`, `create_time`),
  KEY `idx_fill_match` (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单成交明细表';

-- ----------------------------
-- Table structure for ord_order_cancel (Cancel Request Records)
-- ----------------------------
DROP TABLE IF EXISTS `ord_order_cancel`;
CREATE TABLE `ord_order_cancel` (
  `cancel_id`        BIGINT NOT NULL COMMENT '取消ID',
  `order_id`         BIGINT NOT NULL COMMENT '订单ID',
  `reason`           VARCHAR(255) NOT NULL COMMENT '取消原因',
  `idempotency_key`  VARCHAR(128) NOT NULL COMMENT '幂等键',

  `create_time`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by`        VARCHAR(64) NULL COMMENT '创建人',

  PRIMARY KEY (`cancel_id`),
  UNIQUE KEY `uk_cancel_idem` (`idempotency_key`),
  KEY `idx_cancel_order` (`order_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单取消记录表';
