# 🐳 Docker E2E 测试环境 - 完整总结

## 📝 项目概述

已为 Pig 项目创建了一个完整的、可擦除的 E2E 测试基础环境，使用 Docker Compose 编排所有依赖服务。

## ✨ 核心特性

### 1. 一键启动
```bash
./start-test-env.sh
```
自动启动并配置所有服务，无需手动干预。

### 2. 完整的服务栈

| 服务 | 镜像 | 版本 | 端口 | 说明 |
|------|------|------|------|------|
| MySQL | mysql | 8.0 | 33307 | 主数据库，自动导入初始化脚本 |
| Redis | redis | 7-alpine | 36380 | 缓存服务 |
| Kafka | confluentinc/cp-kafka | 7.6.0 | 9093 | 消息队列 |
| Zookeeper | confluentinc/cp-zookeeper | 7.6.0 | 22181 | Kafka 协调服务 |
| Nacos | nacos/nacos-server | v3.1.0 | 8849, 9849 | 配置中心 & 服务注册 |

### 3. 自动化配置

#### MySQL 数据库
- ✅ 自动导入 5 个 SQL 文件，创建 4 个数据库
  - `pig.sql` + `pig_config.sql` + `error_record.sql` → `pig` 数据库
  - `pig_order.sql` → `pig_order` 数据库
  - `vault_schema.sql` → `pig_vault` 数据库
  - Nacos 自动创建 → `nacos_config` 数据库
- ✅ 配置字符集 utf8mb4
- ✅ 健康检查确保启动完成

#### Kafka 主题
- ✅ 自动创建 2 个主题
  - `domain.order` - 订单领域事件（3 分区）
  - `domain.vault` - 金库领域事件（3 分区）
- ✅ 配置适合单机测试的参数

#### Nacos 配置
- ✅ 自动创建 `dev` 命名空间
- ✅ 自动配置 3 个配置文件
  - `application-dev.yml` - 通用配置（连接 pig 数据库）
  - `pig-order-dev.yml` - 订单服务配置（连接 pig_order 数据库）
  - `pig-vault-dev.yml` - 金库服务配置（连接 pig_vault 数据库）
- ✅ 包含数据源、Redis、Kafka、Outbox 等配置

### 4. 健康检查机制

所有服务都配置了健康检查：
- MySQL: `mysqladmin ping`
- Redis: `redis-cli ping`
- Kafka: `kafka-broker-api-versions`
- Nacos: HTTP 健康检查端点
- Zookeeper: `ruok/imok` 检查

### 5. 验证工具

提供了完整的验证脚本：
```bash
./verify-test-env.sh
```
自动检查 16+ 项配置，确保环境正确。

## 📂 文件清单

### 核心配置文件

1. **docker-compose.yml** (8KB)
   - 完整的服务编排配置
   - 包含所有环境变量
   - 配置了健康检查
   - 包含初始化容器

### 脚本文件

2. **start-test-env.sh** (3.3KB)
   - 启动环境的主脚本
   - 等待健康检查
   - 显示服务信息

3. **cleanup-test-env.sh** (1.2KB)
   - 完全清理环境
   - 删除容器、网络、数据卷
   - 清理持久化数据

4. **verify-test-env.sh** (6.3KB)
   - 全面验证环境配置
   - 检查 16+ 项配置
   - 彩色输出，易于识别

### 文档文件

5. **QUICK-START-DOCKER-ENV.md** (7.2KB)
   - 快速开始指南
   - 三步启动流程
   - 常用操作示例

6. **README-TEST-ENV.md** (7.4KB)
   - 完整的环境文档
   - 详细配置说明
   - 命令参考手册

7. **TROUBLESHOOTING.md** (8.7KB)
   - 故障排查指南
   - 常见问题解决方案
   - 调试技巧

8. **DOCKER-ENV-SUMMARY.md** (本文件)
   - 项目总结
   - 架构说明
   - 使用指南

### 参考文件

9. **nacos-config-example.yml** (1.9KB)
   - Nacos 配置示例
   - 用于参考和手动配置

10. **.gitignore**
    - 忽略 Docker 数据目录
    - 忽略日志文件

## 🏗️ 架构设计

### 网络架构

```
┌─────────────────────────────────────────────────────────┐
│              Docker Network: pig-e2e-network            │
│                       (Bridge Mode)                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  MySQL   │  │  Redis   │  │  Nacos   │             │
│  │  :3306   │  │  :6379   │  │  :8848   │             │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘             │
│        │             │              │                   │
│  ┌─────┴──────────────┴──────────────┴────┐            │
│  │          Application Services           │            │
│  └─────────────────┬───────────────────────┘            │
│                    │                                     │
│  ┌─────────────────┴───────────────┐                   │
│  │  Zookeeper  ◄──►  Kafka         │                   │
│  │   :2181          :29092/9093    │                   │
│  └─────────────────────────────────┘                   │
│                                                          │
└─────────────────────────────────────────────────────────┘
         │                │                │
    localhost:33307  localhost:36380  localhost:8849
```

### 启动流程

```
1. 启动脚本执行
   └─> 清理旧容器（如有）
   └─> docker-compose up -d

2. 容器启动顺序（depends_on）
   └─> Zookeeper 启动
   └─> MySQL 启动
       └─> 导入 SQL 文件（/docker-entrypoint-initdb.d/）
   └─> Redis 启动
   └─> Kafka 启动（依赖 Zookeeper）
   └─> Nacos 启动（依赖 MySQL）

3. 初始化容器执行
   └─> kafka-init 容器
       └─> 创建 domain.order 主题
       └─> 创建 domain.vault 主题
   └─> nacos-init 容器
       └─> 创建 dev 命名空间
       └─> 创建 application-dev.yml 配置

4. 健康检查
   └─> 等待所有服务健康检查通过
   └─> 显示服务信息

5. 环境就绪 ✅
```

### 数据流

```
┌──────────────┐
│ Application  │
└──────┬───────┘
       │
       ├──────────────────────────────┐
       │                              │
       ▼                              ▼
┌──────────────┐              ┌──────────────┐
│    Nacos     │              │    MySQL     │
│  (配置中心)   │              │  (数据存储)   │
└──────────────┘              └──────────────┘
       │
       │ 配置
       ▼
┌──────────────┐     事件     ┌──────────────┐
│ Application  │─────────────►│    Kafka     │
│   Service    │              │  (消息队列)   │
└──────┬───────┘              └──────┬───────┘
       │                             │
       │ 缓存                        │ 订阅
       ▼                             ▼
┌──────────────┐              ┌──────────────┐
│    Redis     │              │  Consumer    │
│              │              │  Service     │
└──────────────┘              └──────────────┘
```

## 🎯 使用场景

### 1. 本地开发

```bash
# 启动测试环境
cd pig-e2e-test
./start-test-env.sh

# 启动应用（在另一个终端）
# 应用会自动连接到测试环境的服务
mvn spring-boot:run

# 开发完成后清理
./cleanup-test-env.sh
```

### 2. E2E 测试

```bash
# 启动测试环境
./start-test-env.sh

# 验证环境
./verify-test-env.sh

# 运行 E2E 测试
export PIG_GATEWAY_URL=http://127.0.0.1:9999
mvn -pl pig-e2e-test -Pe2e verify

# 测试完成后清理
./cleanup-test-env.sh
```

### 3. CI/CD 集成

```yaml
# GitHub Actions 示例
- name: Start test environment
  run: |
    cd pig-e2e-test
    ./start-test-env.sh

- name: Verify environment
  run: |
    cd pig-e2e-test
    ./verify-test-env.sh

- name: Run tests
  run: mvn -pl pig-e2e-test -Pe2e verify

- name: Cleanup
  if: always()
  run: |
    cd pig-e2e-test
    ./cleanup-test-env.sh
```

### 4. 问题调试

```bash
# 启动环境
./start-test-env.sh

# 重现问题
# ...

# 查看日志
docker-compose logs -f mysql
docker-compose logs -f kafka

# 进入容器调试
docker exec -it pig-e2e-mysql bash
docker exec -it pig-e2e-kafka bash

# 调试完成后清理
./cleanup-test-env.sh
```

## 🔧 配置定制

### 修改端口

编辑 `docker-compose.yml`：

```yaml
services:
  mysql:
    ports:
      - "33308:3306"  # 修改为其他端口
```

### 修改 JVM 参数

编辑 `docker-compose.yml`：

```yaml
services:
  nacos:
    environment:
      JVM_XMS: 1024m  # 增加内存
      JVM_XMX: 1024m
```

### 添加持久化

取消 `docker-compose.yml` 中的 volumes 注释：

```yaml
services:
  mysql:
    volumes:
      - ./data/mysql:/var/lib/mysql
```

### 修改 Kafka 主题

编辑 `docker-compose.yml` 中的 `kafka-init` 服务：

```yaml
kafka-init:
  command: |
    "
    # 添加新主题
    kafka-topics --create --if-not-exists \
      --bootstrap-server pig-e2e-kafka:29092 \
      --topic your.new.topic \
      --partitions 3 \
      --replication-factor 1
    "
```

## 📊 性能优化

### 加快启动速度

1. **预拉取镜像**
   ```bash
   docker-compose pull
   ```

2. **启用持久化**
   - 首次启动后数据保留
   - 后续启动无需重新初始化

3. **增加 Docker 资源**
   - CPU: 4 核
   - 内存: 8GB

### 减少资源占用

1. **调整服务配置**（已优化）
   - ✅ Nacos JVM 内存已降低到 256M（从 512M）
   - Kafka 分区数已设置为 3（适中）
   - 可选：关闭不需要的服务

2. **使用轻量级镜像**（已应用）
   - ✅ Redis: alpine 版本（轻量级）
   - ✅ MySQL 8.0（标准版本，可用 5.7 进一步优化）

## 🔒 安全说明

⚠️ **重要**: 此环境仅用于测试，不要用于生产！

已禁用的安全特性：
- ❌ MySQL root 远程访问
- ❌ Redis 无密码
- ❌ Nacos 无认证
- ❌ Kafka 无加密

生产环境请：
- ✅ 启用认证和授权
- ✅ 使用 SSL/TLS 加密
- ✅ 配置防火墙规则
- ✅ 使用密钥管理服务

## 📈 监控和日志

### 查看实时日志

```bash
# 所有服务
docker-compose logs -f

# 特定服务
docker-compose logs -f mysql
docker-compose logs -f kafka
```

### 资源监控

```bash
# 实时监控
docker stats

# 单次查看
docker stats --no-stream
```

### 日志位置

| 服务 | 日志位置 |
|------|---------|
| MySQL | `/var/log/mysql/` |
| Redis | stdout（Docker logs） |
| Kafka | `/var/lib/kafka/data/` |
| Nacos | `/home/nacos/logs/` |

## 🚀 后续改进

### 短期计划
- [ ] 添加 Prometheus + Grafana 监控
- [ ] 添加 Kafka Manager 管理界面
- [ ] 支持 ARM 架构（M1/M2 Mac）
- [ ] 添加更多配置模板

### 中期计划
- [ ] 支持多环境切换（dev/test/staging）
- [ ] 集成日志收集（ELK）
- [ ] 添加性能测试支持
- [ ] 支持分布式追踪（Jaeger）

### 长期计划
- [ ] 支持 Kubernetes 部署
- [ ] 添加混沌工程支持
- [ ] 集成 API 网关
- [ ] 完整的可观测性方案

## 📞 支持和反馈

### 文档索引

- **快速开始**: [QUICK-START-DOCKER-ENV.md](QUICK-START-DOCKER-ENV.md)
- **完整文档**: [README-TEST-ENV.md](README-TEST-ENV.md)
- **故障排查**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **项目总览**: [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)

### 常见问题

查看 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) 获取详细的故障排查指南。

### 获取帮助

1. 运行验证脚本: `./verify-test-env.sh`
2. 查看日志: `docker-compose logs`
3. 参考故障排查文档
4. 尝试完全重置: `./cleanup-test-env.sh && ./start-test-env.sh`

## 📝 变更记录

### v1.0.0 (2025-01-22)

#### 新增功能
- ✨ 创建完整的 Docker Compose 配置
- ✨ 自动化启动脚本
- ✨ 环境验证脚本
- ✨ 完整的文档体系
- ✨ 故障排查指南

#### 服务配置
- ✅ MySQL 8.0 + 自动导入 5 个数据库文件
- ✅ Redis 7 + AOF 持久化
- ✅ Kafka 7.6.0 + 自动创建 2 个主题
- ✅ Nacos 3.1.0 + 自动配置
- ✅ Zookeeper 7.6.0

#### 文档
- 📝 快速开始指南
- 📝 完整环境文档
- 📝 故障排查指南
- 📝 配置示例
- 📝 项目总结

## 🎉 总结

现在你已经拥有了一个：

✅ **完整的** - 包含所有依赖服务  
✅ **自动化的** - 一键启动，自动配置  
✅ **可靠的** - 健康检查，验证脚本  
✅ **可擦除的** - 随时清理，快速重建  
✅ **有文档的** - 完整的使用指南  

E2E 测试基础环境！

**立即开始使用：**

```bash
cd pig-e2e-test
./start-test-env.sh
./verify-test-env.sh
```

---

**Happy Testing! 🚀**
