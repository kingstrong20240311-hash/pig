# Pig-Gym 模块 Nacos 配置指南

## 📋 配置概述

本文档说明如何为 pig-gym 健身管理模块配置 Nacos 注册中心和配置中心。

## 🚀 快速配置

### 方式一：SQL 脚本自动配置（推荐）

1. **执行配置脚本**

```bash
# 连接到 MySQL
mysql -u root -p

# 执行配置脚本
source db/add_pig_gym_config.sql
```

2. **验证配置**

登录 Nacos 控制台：http://localhost:8848/nacos (默认账号：nacos/nacos)

检查以下配置是否存在：
- ✅ `pig-gym-biz-dev.yml` - 业务服务配置
- ✅ `pig-gateway-dev.yml` - 包含 gym 路由

### 方式二：Nacos 控制台手动配置

#### 1. 添加 pig-gym-biz-dev.yml

登录 Nacos 控制台 → 配置管理 → 配置列表 → 点击 "+" 创建配置

**配置信息：**
- Data ID: `pig-gym-biz-dev.yml`
- Group: `DEFAULT_GROUP`
- 配置格式: `YAML`
- 配置内容：

```yaml
# 数据源配置
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
```

#### 2. 更新 Gateway 路由配置

找到并编辑 `pig-gateway-dev.yml` 配置，在 `routes` 部分添加：

```yaml
          # 健身管理模块
          - id: pig-gym-biz
            uri: lb://pig-gym-biz
            predicates:
              - Path=/gym/**
```

建议添加位置：在 `pig-vault-biz` 路由之后，`pig-codegen` 路由之前。

## 📦 数据库初始化

### 1. 创建数据库

```bash
mysql -u root -p
source db/pig_gym_schema.sql
```

或手动创建：

```sql
CREATE DATABASE IF NOT EXISTS `pig_gym`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

### 2. 验证数据库

```sql
USE pig_gym;
SHOW TABLES;
```

应该看到以下表：
- gym_member (会员表)
- gym_membership_card (会员卡表)
- gym_checkin (签到记录表)
- gym_coach (教练表)
- gym_course (课程表)
- gym_course_booking (课程预约表)

## 🔧 环境变量配置

如果使用非默认的数据库配置，需要设置以下环境变量：

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=
```

## 🚀 启动服务

### 启动顺序

1. **启动基础设施**
   ```bash
   # MySQL
   # Redis
   # Nacos (pig-register)
   cd pig-register
   mvn spring-boot:run
   ```

2. **启动网关和认证**
   ```bash
   # Gateway
   cd pig-gateway
   mvn spring-boot:run

   # Auth
   cd pig-auth
   mvn spring-boot:run
   ```

3. **启动 gym 服务**
   ```bash
   cd pig-gym/pig-gym-biz
   mvn spring-boot:run
   ```

### 使用 IDEA 启动

1. 打开 `PigGymApplication.java`
2. 右键 → Run 'PigGymApplication'
3. 确保 VM Options 包含：
   ```
   -Dspring.profiles.active=dev
   ```

## ✅ 验证配置

### 1. 检查服务注册

访问 Nacos 控制台：http://localhost:8848/nacos

服务列表应包含：
- ✅ pig-gym-biz

### 2. 检查配置加载

查看服务启动日志，确认以下信息：
```
Located property source: [BootstrapPropertySource {name='bootstrapProperties-pig-gym-biz-dev.yml,DEFAULT_GROUP'}]
```

### 3. 测试服务访问

**直接访问：**
```bash
curl http://localhost:5010/actuator/health
```

**通过网关访问：**
```bash
curl http://localhost:9999/gym/actuator/health
```

**API 文档：**
- 直接访问：http://localhost:5010/doc.html
- 通过网关：http://localhost:9999/gym/doc.html

## 🔍 常见问题

### 问题 1：服务无法注册到 Nacos

**原因：** Nacos 配置错误或 Nacos 未启动

**解决：**
```bash
# 检查 Nacos 是否运行
curl http://localhost:8848/nacos

# 检查 application.yml 中的 Nacos 配置
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_HOST:127.0.0.1}:${NACOS_PORT:8848}
```

### 问题 2：无法加载配置

**原因：** Data ID 或 Group 不匹配

**解决：**
1. 检查 Nacos 控制台中配置的 Data ID 是否为 `pig-gym-biz-dev.yml`
2. 检查 Group 是否为 `DEFAULT_GROUP`
3. 检查服务启动时的 profile 是否为 `dev`

### 问题 3：数据库连接失败

**原因：** 数据库未创建或连接配置错误

**解决：**
```sql
-- 检查数据库是否存在
SHOW DATABASES LIKE 'pig_gym';

-- 创建数据库
CREATE DATABASE pig_gym;

-- 检查用户权限
GRANT ALL PRIVILEGES ON pig_gym.* TO 'root'@'localhost';
```

### 问题 4：Gateway 无法路由到 gym 服务

**原因：** Gateway 路由配置未更新

**解决：**
1. 登录 Nacos 控制台
2. 找到 `pig-gateway-dev.yml` 配置
3. 确认包含 gym 路由配置
4. 重启 pig-gateway 服务

## 📚 配置项说明

### 数据库连接池配置

```yaml
hikari:
  connection-timeout: 30000      # 连接超时时间(ms)
  idle-timeout: 600000           # 空闲超时时间(ms)
  max-lifetime: 1800000          # 最大生命周期(ms)
  maximum-pool-size: 20          # 最大连接数
  minimum-idle: 5                # 最小空闲连接数
```

### Redis 连接池配置

```yaml
lettuce:
  pool:
    max-active: 8    # 最大活动连接数
    max-idle: 8      # 最大空闲连接数
    min-idle: 0      # 最小空闲连接数
    max-wait: -1     # 最大等待时间(-1表示无限制)
```

### MyBatis Plus 配置

```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: auto                # 主键策略：自增
      logic-delete-field: delFlag  # 逻辑删除字段
      logic-delete-value: 1        # 逻辑删除值
      logic-not-delete-value: 0    # 未删除值
```

## 🔐 安全配置

### 生产环境配置建议

1. **加密敏感信息**

使用 Jasypt 加密数据库密码：
```yaml
spring:
  datasource:
    password: ENC(加密后的密码)
```

2. **限制访问权限**

在 Gateway 中配置：
```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 100
      redis-rate-limiter.burstCapacity: 200
```

## 📞 技术支持

如遇到配置问题，请查看：
- 项目文档：[wiki.pig4cloud.com](https://wiki.pig4cloud.com)
- 项目仓库：查看 Issues
- 服务日志：`logs/pig-gym-biz.log`
