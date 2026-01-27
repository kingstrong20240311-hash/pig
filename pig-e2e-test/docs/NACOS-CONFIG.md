# Nacos 配置说明

## 📚 概述

Nacos 配置通过 `db/pig_config.sql` 文件自动导入到数据库中。这个文件包含了完整的 Nacos 数据库结构和所有服务的配置数据。

## 🗄️ pig_config.sql 内容

### 数据库信息
- **数据库名**: `pig_config`
- **文件大小**: ~59KB
- **记录数**: 424 行
- **导入时机**: MySQL 容器启动时自动执行

### 包含的表结构

| 表名 | 说明 | 记录数 |
|------|------|--------|
| `config_info` | 配置信息表 | 12 条配置 |
| `config_info_beta` | Beta 配置 | 0 |
| `config_info_gray` | 灰度配置 | 0 |
| `config_info_tag` | 标签配置 | 0 |
| `config_tags_relation` | 配置标签关系 | 0 |
| `group_capacity` | 分组容量 | 1 |
| `his_config_info` | 历史配置 | 2 |
| `permissions` | 权限表 | 0 |
| `roles` | 角色表 | 1 (ROLE_ADMIN) |
| `tenant_capacity` | 租户容量 | 1 |
| `tenant_info` | 租户信息 | 0 |
| `users` | 用户表 | 1 (nacos) |

### 包含的配置文件

`pig_config.sql` 包含以下服务配置（已导入到 `config_info` 表）：

#### 1. application-dev.yml
**说明**: 通用应用配置

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      password: ${REDIS_PASSWORD:}
      port: ${REDIS_PORT:6379}
  cloud:
    sentinel:
      eager: true
    openfeign:
      sentinel:
        enabled: true

mybatis-plus:
  mapper-locations: classpath:/mapper/*Mapper.xml
  global-config:
    banner: false
    db-config:
      id-type: auto
      logic-delete-value: 1
      logic-not-delete-value: 0
```

#### 2. pig-auth-dev.yml
**说明**: 认证服务配置

```yaml
spring:
  freemarker:
    cache: true
    charset: UTF-8
    content-type: text/html

security:
  encode-key: 'thanks,pig4cloud'
  ignore-clients:
    - test
    - client
    - open
    - app
```

#### 3. pig-gateway-dev.yml
**说明**: 网关路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: pig-auth
          uri: lb://pig-auth
          predicates:
            - Path=/auth/**
        - id: pig-upms-biz
          uri: lb://pig-upms-biz
          predicates:
            - Path=/admin/**
```

#### 4. pig-upms-biz-dev.yml
**说明**: 用户权限管理服务

```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DB:pig}?...
```

#### 5. pig-order-biz-dev.yml ⭐
**说明**: 订单服务配置（连接 pig_order 数据库）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/pig_order?...
    hikari:
      maximum-pool-size: 20
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}

mybatis-plus:
  mapper-locations: classpath:/mapper/*Mapper.xml
  type-aliases-package: com.pig4cloud.pig.order.api.entity
```

#### 6. pig-vault-biz-dev.yml ⭐
**说明**: 金库服务配置（连接 pig_vault 数据库）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/pig_vault?...
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
```

#### 7. pig-codegen-dev.yml
**说明**: 代码生成服务

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/pig?...
```

#### 8. pig-quartz-dev.yml
**说明**: 定时任务服务

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/pig?...
```

#### 9. pig-monitor-dev.yml
**说明**: 监控服务

```yaml
spring:
  security:
    user:
      name: ENC(8Hk2ILNJM8UTOuW/Xi75qg==)     # pig
      password: ENC(o6cuPFfUevmTbkmBnE67Ow====) # pig
```

## 🚀 配置加载流程

```
1. Docker Compose 启动
   ↓
2. MySQL 容器启动
   ↓
3. 执行初始化脚本（按顺序）
   - 01-pig.sql
   - 02-pig_config.sql ⭐ 
   - 03-pig_order.sql
   - 04-vault_schema.sql
   - 05-error_record.sql
   ↓
4. pig_config 数据库创建完成
   - 表结构创建
   - 配置数据导入
   ↓
5. Nacos 容器启动
   ↓
6. Nacos 连接到 pig_config 数据库
   ↓
7. 读取数据库中的配置
   ↓
8. 配置加载完成 ✅
```

## 🔧 Docker Compose 配置

### MySQL 挂载
```yaml
mysql:
  volumes:
    - ../db/pig.sql:/docker-entrypoint-initdb.d/01-pig.sql
    - ../db/pig_config.sql:/docker-entrypoint-initdb.d/02-pig_config.sql  # Nacos 配置
    - ../db/pig_order.sql:/docker-entrypoint-initdb.d/03-pig_order.sql
    - ../db/vault_schema.sql:/docker-entrypoint-initdb.d/04-vault_schema.sql
    - ../db/error_record.sql:/docker-entrypoint-initdb.d/05-error_record.sql
```

### Nacos 数据源配置
```yaml
nacos:
  environment:
    SPRING_DATASOURCE_PLATFORM: mysql
    MYSQL_SERVICE_HOST: pig-e2e-mysql
    MYSQL_SERVICE_PORT: 3306
    MYSQL_SERVICE_DB_NAME: pig_config  # 连接到 pig_config 数据库
    MYSQL_SERVICE_USER: root
    MYSQL_SERVICE_PASSWORD: root
```

## 🔍 验证配置

### 方法 1: 通过 Nacos 控制台

1. 访问 http://localhost:8849/nacos
2. 登录（nacos/nacos）
3. 进入 **配置管理** → **配置列表**
4. 选择 **public** 命名空间
5. 查看配置列表

### 方法 2: 通过数据库

```bash
# 连接到 MySQL
docker exec -it pig-e2e-mysql mysql -uroot -proot

# 切换到 pig_config 数据库
USE pig_config;

# 查看配置列表
SELECT id, data_id, group_id, tenant_id FROM config_info;

# 查看特定配置内容
SELECT content FROM config_info WHERE data_id = 'pig-order-biz-dev.yml';
```

### 方法 3: 通过 API

```bash
# 获取配置列表
curl 'http://localhost:8849/nacos/v1/cs/configs?pageNo=1&pageSize=100'

# 获取特定配置
curl 'http://localhost:8849/nacos/v1/cs/configs?dataId=pig-order-biz-dev.yml&group=DEFAULT_GROUP&tenant=public'
```

## 📝 修改配置

### 方式 1: 通过 Nacos 控制台（推荐）

1. 登录 Nacos 控制台
2. 进入配置管理
3. 点击编辑按钮
4. 修改配置内容
5. 发布配置

### 方式 2: 修改 pig_config.sql

如果需要永久修改配置：

1. 编辑 `db/pig_config.sql` 文件
2. 找到对应的 INSERT 语句
3. 修改 content 字段的内容
4. 重新启动环境：
   ```bash
   ./cleanup-test-env.sh
   ./start-test-env.sh
   ```

⚠️ **注意**: 修改 SQL 文件后需要完全重建环境才能生效。

### 方式 3: 通过 API

```bash
# 更新配置
curl -X POST 'http://localhost:8849/nacos/v1/cs/configs' \
  -d 'dataId=pig-order-biz-dev.yml' \
  -d 'group=DEFAULT_GROUP' \
  -d 'tenant=public' \
  -d 'content=...'
```

## 🔐 Nacos 用户信息

从 `pig_config.sql` 导入的用户信息：

| 用户名 | 密码（加密） | 角色 |
|--------|-------------|------|
| nacos | $2a$10$W6PKgRTzXUp6R/NY853Kn... | ROLE_ADMIN |

**登录信息**:
- 用户名: `nacos`
- 密码: `nacos`

## 💡 最佳实践

### 1. 不要手动创建配置

❌ **错误做法**: 在 docker-compose.yml 中通过 API 创建配置
```yaml
# 不推荐
nacos-init:
  command: |
    curl -X POST 'http://nacos:8848/nacos/v1/cs/configs' ...
```

✅ **正确做法**: 使用 `pig_config.sql` 导入配置
- 配置统一管理
- 可版本控制
- 可重复构建

### 2. 环境变量支持

配置中使用环境变量占位符：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/...
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    password: ${REDIS_PASSWORD:}
```

在 Docker Compose 中设置：
```yaml
environment:
  MYSQL_HOST: pig-e2e-mysql
  REDIS_HOST: pig-e2e-redis
```

### 3. 测试环境隔离

使用不同的命名空间：
- `public` - 默认命名空间
- `dev` - 开发环境
- `test` - 测试环境

## 🐛 常见问题

### Q1: Nacos 启动后看不到配置？

**原因**: `pig_config.sql` 未正确导入

**解决**:
```bash
# 检查数据库
docker exec pig-e2e-mysql mysql -uroot -proot -e "SELECT COUNT(*) FROM pig_config.config_info"

# 如果为 0，重新启动
./cleanup-test-env.sh
./start-test-env.sh
```

### Q2: 配置修改后不生效？

**原因**: 
1. 未重启服务
2. 配置缓存

**解决**:
1. 在 Nacos 控制台点击"发布"
2. 重启相关服务
3. 检查服务是否正确监听配置变化

### Q3: 如何备份配置？

```bash
# 导出配置
docker exec pig-e2e-mysql mysqldump -uroot -proot pig_config > pig_config_backup.sql
```

## 📚 相关文档

- [README-TEST-ENV.md](README-TEST-ENV.md) - 环境文档
- [DATABASE-CHANGES.md](DATABASE-CHANGES.md) - 数据库变更
- [QUICK-START-DOCKER-ENV.md](QUICK-START-DOCKER-ENV.md) - 快速开始

---

**总结**: `pig_config.sql` 包含了完整的 Nacos 配置数据，在容器启动时自动导入，无需手动配置。✅
