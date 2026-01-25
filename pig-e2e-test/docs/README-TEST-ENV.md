# Pig E2E 测试环境文档

## 📚 概述

此目录包含 Pig 项目的端到端（E2E）测试基础环境配置。使用 Docker Compose 提供一个完整的、可擦除的测试环境。

## 🎯 环境组件

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| MySQL | mysql:8.0 | 33307 | 主数据库 |
| Redis | redis:7-alpine | 36380 | 缓存服务 |
| Kafka | confluentinc/cp-kafka:7.6.0 | 9093 | 消息队列 |
| Zookeeper | confluentinc/cp-zookeeper:7.6.0 | 22181 | Kafka 协调服务 |
| Nacos | nacos/nacos-server:v3.1.0 | 8849, 9849 | 配置中心 & 服务注册 |

## 🚀 快速开始

### 1. 启动测试环境

```bash
cd pig-e2e-test
./start-test-env.sh
```

启动脚本会自动：
- ✅ 启动所有服务容器
- ✅ 等待服务健康检查通过
- ✅ 自动导入数据库初始化脚本
  - `pig.sql` + `pig_config.sql` (包含 Nacos 配置) + `error_record.sql`
  - `pig_order.sql`, `vault_schema.sql`
- ✅ 创建 Kafka 主题（domain.order, domain.vault）
- ✅ Nacos 从数据库加载配置（无需手动配置）

### 2. 验证服务

启动完成后，可以访问：

- **Nacos 控制台**: http://localhost:8849/nacos
  - 用户名: `nacos`
  - 密码: `nacos`

- **MySQL 数据库**: `localhost:33307`
  - 用户: `root`
  - 密码: `root`
  - 数据库: `pig` (主数据库), `pig_order` (订单), `pig_vault` (金库), `nacos_config` (Nacos配置)

- **Redis**: `localhost:36380`

- **Kafka**: `localhost:9093`

### 3. 运行 E2E 测试

```bash
# 设置 Gateway 地址
export PIG_GATEWAY_URL=http://127.0.0.1:9999

# 运行所有 E2E 测试
mvn -pl pig-e2e-test -Pe2e verify

# 运行特定测试
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=OrderFlowE2eTest
```

### 4. 停止测试环境

```bash
# 停止但保留数据
cd pig-e2e-test
docker-compose down

# 完全清理（删除容器、网络、数据卷）
./cleanup-test-env.sh
```

## 📁 文件结构

```
pig-e2e-test/
├── docker-compose.yml          # Docker Compose 配置文件
├── start-test-env.sh          # 启动脚本
├── cleanup-test-env.sh        # 清理脚本
├── README-TEST-ENV.md         # 本文档
├── env.template               # 环境变量模板
└── src/
    └── test/
        └── java/              # E2E 测试代码
```

## 🔧 配置说明

### MySQL

- **端口**: 33307（避免与本地 MySQL 冲突）
- **root 密码**: root
- **字符集**: utf8mb4
- **自动导入**（按顺序）:
  1. `pig.sql` - 主数据库（pig）- 用户、角色、权限等
  2. `pig_config.sql` - **Nacos 配置数据库（pig_config）** ⭐ 包含所有配置
  3. `pig_order.sql` - 订单数据库（pig_order）- 订单、市场、成交等
  4. `vault_schema.sql` - 金库数据库（pig_vault）- 账户、余额等
  5. `error_record.sql` - 错误记录表（pig.error_record）

### Redis

- **端口**: 36380（避免与本地 Redis 冲突）
- **无密码认证**
- **启用 AOF 持久化**（可选）

### Kafka

- **端口**: 9093（broker）
- **Zookeeper 端口**: 22181
- **自动创建主题**:
  - `domain.order` - 订单领域事件（3 分区）
  - `domain.vault` - 金库领域事件（3 分区）
- **Replication Factor**: 1（单机测试环境）

### Nacos

- **Web 端口**: 8849
- **gRPC 端口**: 9849
- **模式**: standalone
- **认证**: 已禁用（测试环境）
- **数据库**: 使用 MySQL `pig_config` 数据库存储配置
- **内存配置**: 256M 堆内存（已优化，适合测试环境）
- **配置加载**: 从 `pig_config.sql` 自动导入 ⭐
  - **包含配置**:
    - `application-dev.yml` - 通用应用配置
    - `pig-auth-dev.yml` - 认证服务
    - `pig-gateway-dev.yml` - 网关配置
    - `pig-upms-biz-dev.yml` - 用户权限管理
    - `pig-order-biz-dev.yml` - 订单服务（→ pig_order 数据库）
    - `pig-vault-biz-dev.yml` - 金库服务（→ pig_vault 数据库）
    - `pig-codegen-dev.yml` - 代码生成
    - `pig-quartz-dev.yml` - 定时任务
    - `pig-monitor-dev.yml` - 监控服务

## 🛠️ 常用命令

### 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f kafka
docker-compose logs -f nacos
```

### 进入容器

```bash
# 进入 MySQL 容器
docker exec -it pig-e2e-mysql bash
mysql -uroot -proot

# 进入 Redis 容器
docker exec -it pig-e2e-redis redis-cli

# 进入 Kafka 容器
docker exec -it pig-e2e-kafka bash
```

### 验证 Kafka 主题

```bash
# 列出所有主题
docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093

# 查看主题详情
docker exec pig-e2e-kafka kafka-topics --describe --topic domain.order --bootstrap-server localhost:9093

# 查看消息（从头消费）
docker exec pig-e2e-kafka kafka-console-consumer --bootstrap-server localhost:9093 --topic domain.order --from-beginning
```

### 验证 MySQL 数据

```bash
# 连接到 MySQL
docker exec -it pig-e2e-mysql mysql -uroot -proot

# 查看数据库
SHOW DATABASES;

# 查看表
USE pig;
SHOW TABLES;

# 验证数据
SELECT * FROM sys_user LIMIT 5;
```

### 验证 Nacos 配置

```bash
# 查看配置列表
curl "http://localhost:8849/nacos/v1/cs/configs?tenant=dev&group=DEFAULT_GROUP"

# 获取特定配置
curl "http://localhost:8849/nacos/v1/cs/configs?dataId=application-dev.yml&group=DEFAULT_GROUP&tenant=dev"
```

## 🐛 故障排查

### MySQL 启动失败

**问题**: MySQL 容器启动失败或健康检查失败

**解决**:
```bash
# 查看日志
docker-compose logs mysql

# 清理数据并重启
./cleanup-test-env.sh
./start-test-env.sh
```

### Kafka 连接失败

**问题**: 应用无法连接到 Kafka

**解决**:
1. 确认 Kafka 容器正在运行:
   ```bash
   docker ps | grep kafka
   ```

2. 验证 Kafka 主题是否创建:
   ```bash
   docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093
   ```

3. 检查防火墙设置，确保端口 9093 可访问

### Nacos 初始化失败

**问题**: Nacos 配置未正确初始化

**解决**:
1. 查看 nacos-init 容器日志:
   ```bash
   docker-compose logs nacos-init
   ```

2. 手动初始化配置:
   - 访问 http://localhost:8849/nacos
   - 登录（nacos/nacos）
   - 手动创建命名空间和配置

### 端口冲突

**问题**: 端口已被占用

**解决**:
1. 修改 `docker-compose.yml` 中的端口映射
2. 或停止占用端口的服务

## 🔐 安全说明

⚠️ **警告**: 此环境仅用于本地测试，不要用于生产环境！

安全特性（已禁用）:
- MySQL root 用户允许远程访问
- Redis 无密码认证
- Nacos 认证已禁用
- Kafka 无 SSL/SASL 认证

## 📊 性能优化

### 加快启动速度

1. **使用本地镜像缓存**:
   ```bash
   # 提前拉取镜像
   docker-compose pull
   ```

2. **启用数据持久化**（可选）:
   - 取消 `docker-compose.yml` 中 volumes 的注释
   - 后续启动会更快（数据已存在）

3. **减少内存使用**:
   - 调整 Nacos JVM 参数（已优化为 512M）
   - 减少 Kafka 分区数量

## 🔄 CI/CD 集成

### GitHub Actions 示例

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  e2e-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Start test environment
        run: |
          cd pig-e2e-test
          ./start-test-env.sh
      
      - name: Run E2E tests
        env:
          PIG_GATEWAY_URL: http://127.0.0.1:9999
        run: mvn -pl pig-e2e-test -Pe2e verify
      
      - name: Cleanup
        if: always()
        run: |
          cd pig-e2e-test
          ./cleanup-test-env.sh
```

## 📞 技术支持

- **项目文档**: [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)
- **快速开始**: [QUICKSTART.md](QUICKSTART.md)
- **环境变量**: [env.template](env.template)

## 📝 变更日志

### v1.0.0 (2025-01-22)
- ✨ 初始版本
- ✅ MySQL 8.0 + 自动导入数据库
- ✅ Redis 7
- ✅ Kafka 7.6.0 + 自动创建主题
- ✅ Nacos 3.1.0 + 自动配置
- ✅ 健康检查和启动脚本

## 📄 许可证

Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0.
