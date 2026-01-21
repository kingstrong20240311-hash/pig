-- Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- ----------------------------
-- Table structure for error_record
-- ----------------------------
DROP TABLE IF EXISTS `error_record`;
CREATE TABLE `error_record` (
    `id` BIGINT(20) NOT NULL COMMENT '主键ID',
    `error_id` VARCHAR(64) NOT NULL COMMENT '业务唯一ID',
    `domain` VARCHAR(64) NOT NULL COMMENT '领域',
    `handler_key` VARCHAR(255) NOT NULL COMMENT '处理函数标识',
    `payload_json` TEXT COMMENT '原始数据JSON',
    `payload_class` VARCHAR(255) COMMENT '强类型反序列化类名',
    `status` VARCHAR(32) NOT NULL COMMENT '状态：NEW/RETRYING/RESOLVED/DEAD',
    `attempts` INT(11) NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `next_retry_time` TIMESTAMP(3) NULL COMMENT '下次重试时间',
    `error_message` VARCHAR(500) COMMENT '错误摘要',
    `stack_trace` TEXT COMMENT '堆栈信息',
    `last_error_at` TIMESTAMP(3) NULL COMMENT '最后一次错误时间',
    `tags` VARCHAR(500) COMMENT '扩展标签（JSON格式）',
    `created_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_error_id` (`error_id`),
    KEY `idx_domain` (`domain`),
    KEY `idx_handler_key` (`handler_key`),
    KEY `idx_status` (`status`),
    KEY `idx_next_retry_time` (`next_retry_time`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错误记录表';
