-- ================================================
-- pig-gym 模块 Nacos 配置脚本
-- 使用说明：
-- 1. 确保 Nacos 已启动
-- 2. 将此 SQL 导入到 pig_config 数据库
-- 3. 重启相关服务使配置生效
-- ================================================

USE `pig_config`;

-- ================================================
-- 1. 添加 pig-gym-biz-dev.yml 配置
-- ================================================
INSERT INTO `config_info` (`data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`)
VALUES (
    'pig-gym-biz-dev.yml',
    'DEFAULT_GROUP',
    '# 数据源配置
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/pig_gym?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true
    hikari:
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      maximum-pool-size: 20
      minimum-idle: 5

  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 10000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1

# MyBatis Plus 配置
mybatis-plus:
  mapper-locations: classpath:/mapper/*Mapper.xml
  type-aliases-package: com.pig4cloud.pig.gym.entity
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: delFlag
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    call-setters-on-nulls: true
    jdbc-type-for-null: null

# 日志配置
logging:
  level:
    com.pig4cloud.pig.gym: debug
    com.pig4cloud.pig.gym.mapper: debug
',
    '8e7c7e88c1f44e0f3f9a0e6e5e7c7e88',
    NOW(),
    NOW(),
    'admin',
    '127.0.0.1',
    '',
    'public',
    'pig-gym 健身管理模块配置',
    NULL,
    NULL,
    'yaml',
    NULL,
    ''
)
ON DUPLICATE KEY UPDATE
    `content` = VALUES(`content`),
    `md5` = VALUES(`md5`),
    `gmt_modified` = NOW(),
    `src_user` = 'admin';

-- ================================================
-- 2. 更新 Gateway 路由配置，添加 gym 路由
-- ================================================
-- 注意：此更新语句会在现有的 vault 路由后添加 gym 路由
UPDATE `config_info`
SET
    `content` = REPLACE(
        `content`,
        '          # 资产模块
          - id: pig-vault-biz
            uri: lb://pig-vault-biz
            predicates:
              - Path=/vault/**',
        '          # 资产模块
          - id: pig-vault-biz
            uri: lb://pig-vault-biz
            predicates:
              - Path=/vault/**
          # 健身管理模块
          - id: pig-gym-biz
            uri: lb://pig-gym-biz
            predicates:
              - Path=/gym/**'
    ),
    `md5` = MD5(`content`),
    `gmt_modified` = NOW(),
    `src_user` = 'admin'
WHERE
    `data_id` = 'pig-gateway-dev.yml'
    AND `group_id` = 'DEFAULT_GROUP'
    AND `tenant_id` = 'public'
    AND `content` NOT LIKE '%pig-gym-biz%';

-- ================================================
-- 3. 验证配置是否添加成功
-- ================================================
SELECT
    '✅ 配置检查' AS status,
    COUNT(*) AS gym_config_count,
    CASE
        WHEN COUNT(*) > 0 THEN '✅ pig-gym-biz-dev.yml 配置已添加'
        ELSE '❌ pig-gym-biz-dev.yml 配置添加失败'
    END AS result
FROM `config_info`
WHERE `data_id` = 'pig-gym-biz-dev.yml'
    AND `group_id` = 'DEFAULT_GROUP';

SELECT
    '✅ Gateway 路由检查' AS status,
    CASE
        WHEN `content` LIKE '%pig-gym-biz%' THEN '✅ Gateway 路由已更新'
        ELSE '⚠️  需要手动更新 Gateway 路由'
    END AS result
FROM `config_info`
WHERE `data_id` = 'pig-gateway-dev.yml'
    AND `group_id` = 'DEFAULT_GROUP'
    AND `tenant_id` = 'public'
LIMIT 1;

-- ================================================
-- 配置完成说明
-- ================================================
/*
📋 配置完成清单：

✅ 1. pig-gym-biz-dev.yml 配置已添加
   - 数据源：pig_gym 数据库
   - Redis 连接配置
   - MyBatis Plus 配置
   - 日志配置

✅ 2. Gateway 路由已更新
   - 路由 ID: pig-gym-biz
   - 路径: /gym/**
   - 负载均衡: lb://pig-gym-biz

📌 后续步骤：

1. 创建数据库：
   CREATE DATABASE IF NOT EXISTS `pig_gym` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

2. 重启服务（按顺序）：
   - pig-gateway (网关需要加载新路由)
   - pig-gym-biz (新服务首次启动)

3. 验证配置：
   - Nacos 控制台：http://localhost:8848/nacos (nacos/nacos)
   - 检查配置列表中是否有 pig-gym-biz-dev.yml
   - 检查 pig-gateway-dev.yml 是否包含 gym 路由

4. 测试访问：
   - 直接访问: http://localhost:5010/doc.html
   - 通过网关: http://localhost:9999/gym/doc.html

🔧 如需自定义配置：
   登录 Nacos 控制台 → 配置管理 → 配置列表 → 编辑 pig-gym-biz-dev.yml
*/
