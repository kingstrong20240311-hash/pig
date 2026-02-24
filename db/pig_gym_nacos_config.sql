-- pig-gym 模块的 Nacos 配置
-- 使用说明：将此 SQL 导入到 pig_config 数据库

USE `pig_config`;

-- 1. pig-gym-biz-dev.yml 配置 (业务服务配置)
INSERT INTO `config_info` (`data_id`, `group_id`, `content`, `md5`, `gmt_create`, `gmt_modified`, `src_user`, `src_ip`, `app_name`, `tenant_id`, `c_desc`, `c_use`, `effect`, `type`, `c_schema`, `encrypted_data_key`)
VALUES (
    'pig-gym-biz-dev.yml',
    'DEFAULT_GROUP',
    '# 数据源\nspring:\n  datasource:\n    type: com.zaxxer.hikari.HikariDataSource\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    username: ${MYSQL_USERNAME:root}\n    password: ${MYSQL_PASSWORD:root}\n    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/pig_gym?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true\n    hikari:\n      connection-timeout: 30000\n      idle-timeout: 600000\n      max-lifetime: 1800000\n      maximum-pool-size: 20\n      minimum-idle: 5\n\n  data:\n    redis:\n      host: ${REDIS_HOST:127.0.0.1}\n      port: ${REDIS_PORT:6379}\n      password: ${REDIS_PASSWORD:}\n      database: 0\n      timeout: 10000\n      lettuce:\n        pool:\n          max-active: 8\n          max-idle: 8\n          min-idle: 0\n          max-wait: -1\n\n# MyBatis Plus 配置\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml\n  type-aliases-package: com.pig4cloud.pig.gym.api.entity\n  global-config:\n    db-config:\n      id-type: auto\n      logic-delete-field: delFlag\n      logic-delete-value: 1\n      logic-not-delete-value: 0\n  configuration:\n    map-underscore-to-camel-case: true\n    cache-enabled: false\n    call-setters-on-nulls: true\n    jdbc-type-for-null: null\n\n# 日志配置\nlogging:\n  level:\n    com.pig4cloud.pig.gym: info\n    com.pig4cloud.pig.gym.mapper: info\n',
    MD5('# 数据源\nspring:\n  datasource:\n    type: com.zaxxer.hikari.HikariDataSource\n    driver-class-name: com.mysql.cj.jdbc.Driver\n    username: ${MYSQL_USERNAME:root}\n    password: ${MYSQL_PASSWORD:root}\n    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/pig_gym?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true\n    hikari:\n      connection-timeout: 30000\n      idle-timeout: 600000\n      max-lifetime: 1800000\n      maximum-pool-size: 20\n      minimum-idle: 5\n\n  data:\n    redis:\n      host: ${REDIS_HOST:127.0.0.1}\n      port: ${REDIS_PORT:6379}\n      password: ${REDIS_PASSWORD:}\n      database: 0\n      timeout: 10000\n      lettuce:\n        pool:\n          max-active: 8\n          max-idle: 8\n          min-idle: 0\n          max-wait: -1\n\n# MyBatis Plus 配置\nmybatis-plus:\n  mapper-locations: classpath:/mapper/*Mapper.xml\n  type-aliases-package: com.pig4cloud.pig.gym.api.entity\n  global-config:\n    db-config:\n      id-type: auto\n      logic-delete-field: delFlag\n      logic-delete-value: 1\n      logic-not-delete-value: 0\n  configuration:\n    map-underscore-to-camel-case: true\n    cache-enabled: false\n    call-setters-on-nulls: true\n    jdbc-type-for-null: null\n\n# 日志配置\nlogging:\n  level:\n    com.pig4cloud.pig.gym: info\n    com.pig4cloud.pig.gym.mapper: info\n'),
    NOW(),
    NOW(),
    'nacos',
    '127.0.0.1',
    '',
    'public',
    'pig-gym-biz datasource config',
    NULL,
    NULL,
    'yaml',
    NULL,
    ''
);

-- 2. 更新 pig-gateway-dev.yml，添加 gym 模块路由
-- 注意：这需要手动在 Nacos 控制台更新 pig-gateway-dev.yml，在 routes 部分添加以下内容：
/*
          # 健身管理模块
          - id: pig-gym-biz
            uri: lb://pig-gym-biz
            predicates:
              - Path=/gym/**
*/

-- 配置导入完成提示
SELECT '✅ pig-gym Nacos 配置已生成' AS status,
       'pig-gym-biz-dev.yml' AS config_file,
       '请注意：还需要手动更新 pig-gateway-dev.yml 添加路由配置' AS note;
