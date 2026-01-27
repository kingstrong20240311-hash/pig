# 🚀 E2E 测试环境快速开始指南

## 📋 前置要求

在开始之前，请确保已安装：

- ✅ Docker Desktop（或 Docker Engine）
- ✅ Docker Compose v1.27+
- ✅ 至少 3GB 可用内存（已优化，推荐 4GB+）
- ✅ 至少 10GB 可用磁盘空间

验证安装：
```bash
docker --version
docker-compose --version
```

💡 **内存优化**: Java 服务（Nacos）已优化到 256M，整体环境约需 1.2GB 内存

## 🎯 三步启动

### 步骤 1: 启动测试环境

```bash
cd pig-e2e-test
./start-test-env.sh
```

该脚本会自动：
- 启动所有依赖服务（MySQL, Redis, Kafka, Nacos）
- 导入数据库初始化脚本
- 创建 Kafka 主题
- 配置 Nacos
- 等待所有服务就绪

⏱️ **预计耗时**: 2-3 分钟（首次启动可能需要下载镜像）

### 步骤 2: 验证环境

```bash
./verify-test-env.sh
```

该脚本会检查：
- ✓ 所有容器是否运行
- ✓ 端口是否可访问
- ✓ 数据库是否初始化
- ✓ Kafka 主题是否创建
- ✓ Nacos 配置是否正确

### 步骤 3: 运行测试

```bash
# 设置环境变量
export PIG_GATEWAY_URL=http://127.0.0.1:9999

# 运行 E2E 测试
mvn -pl pig-e2e-test -Pe2e verify
```

## 🎉 就这么简单！

## 📊 服务访问地址

启动成功后，可以通过以下地址访问服务：

| 服务 | 地址 | 凭据 |
|------|------|------|
| **Nacos 控制台** | http://localhost:8849/nacos | nacos / nacos |
| **MySQL** | localhost:33307 | root / root |
| **Redis** | localhost:36380 | 无密码 |
| **Kafka** | localhost:9093 | - |

## 🗂️ 数据库说明

### 主要数据库

1. **pig** - 主应用数据库
   - 用户、角色、权限、错误记录等
   - 初始化自: `../db/pig.sql`, `../db/error_record.sql`

2. **pig_order** - 订单数据库
   - 订单、市场、成交、撤单记录等
   - 初始化自: `../db/pig_order.sql`

3. **pig_vault** - 金库数据库
   - 账户、资产、余额、冻结、账本等
   - 初始化自: `../db/vault_schema.sql`

4. **pig_config** - Nacos 配置数据库 ⭐
   - 初始化自: `../db/pig_config.sql`
   - **包含所有 Nacos 表结构和配置数据**
   - 包含 9+ 个服务配置文件

### 连接数据库

```bash
# 使用 Docker 客户端
docker exec -it pig-e2e-mysql mysql -uroot -proot

# 或使用本地客户端
mysql -h 127.0.0.1 -P 33307 -uroot -proot
```

## 📨 Kafka 主题

系统会自动创建以下主题：

1. **domain.order** - 订单领域事件
   - 3 个分区
   - 用于订单相关的事件流

2. **domain.vault** - 金库领域事件
   - 3 个分区
   - 用于金库相关的事件流

### 查看 Kafka 消息

```bash
# 消费 order 主题
docker exec pig-e2e-kafka kafka-console-consumer \
  --bootstrap-server localhost:9093 \
  --topic domain.order \
  --from-beginning

# 消费 vault 主题
docker exec pig-e2e-kafka kafka-console-consumer \
  --bootstrap-server localhost:9093 \
  --topic domain.vault \
  --from-beginning
```

## ⚙️ Nacos 配置

### 命名空间

- **dev** - 开发环境命名空间

### 配置项

- **application-dev.yml** - 应用通用配置
  - 数据源配置
  - Redis 配置
  - Kafka 配置

### 查看配置

1. 访问 Nacos 控制台: http://localhost:8849/nacos
2. 登录（nacos/nacos）
3. 进入"配置管理" -> "配置列表"
4. 选择 "dev" 命名空间

## 🔄 常用操作

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务
docker-compose logs -f mysql
docker-compose logs -f kafka
docker-compose logs -f nacos
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart mysql
```

### 停止环境

```bash
# 停止但保留数据
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 完全清理

```bash
./cleanup-test-env.sh
```

这会删除：
- 所有容器
- 所有网络
- 所有数据卷
- 持久化数据目录

## 🐛 遇到问题？

### 快速诊断

```bash
# 1. 检查容器状态
docker-compose ps

# 2. 运行验证脚本
./verify-test-env.sh

# 3. 查看失败服务的日志
docker-compose logs [service-name]
```

### 常见问题

| 问题 | 解决方案 |
|------|---------|
| 端口冲突 | 停止占用端口的服务，或修改 `docker-compose.yml` 中的端口 |
| 容器启动失败 | 运行 `./cleanup-test-env.sh` 后重新启动 |
| 数据库未初始化 | 检查 `../db/` 目录下的 SQL 文件是否存在 |
| Kafka 连接失败 | 等待更长时间，Kafka 启动较慢 |

### 详细故障排查

参考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) 获取详细的故障排查指南。

## 📁 相关文档

- **[README-TEST-ENV.md](README-TEST-ENV.md)** - 完整的环境文档
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - 故障排查指南
- **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)** - 项目总览
- **[nacos-config-example.yml](nacos-config-example.yml)** - Nacos 配置示例

## 💡 最佳实践

### 开发流程

1. **启动环境**: `./start-test-env.sh`
2. **验证环境**: `./verify-test-env.sh`
3. **开发/测试**: 运行应用或测试
4. **查看日志**: `docker-compose logs -f`
5. **清理环境**: `./cleanup-test-env.sh`（完成后）

### 日常使用

```bash
# 工作开始时
cd pig-e2e-test
./start-test-env.sh

# 工作期间
docker-compose logs -f [service]  # 需要时查看日志

# 工作结束时
docker-compose down  # 保留数据，下次启动更快
# 或
./cleanup-test-env.sh  # 完全清理
```

### 调试技巧

```bash
# 1. 进入容器内部调试
docker exec -it pig-e2e-mysql bash
docker exec -it pig-e2e-kafka bash

# 2. 实时监控资源使用
docker stats

# 3. 检查网络连通性
docker network inspect pig-e2e-network
```

## 🚀 下一步

环境启动成功后，你可以：

1. ✅ 运行 E2E 测试验证业务流程
2. ✅ 启动应用服务连接到测试环境
3. ✅ 开发新功能并快速验证
4. ✅ 调试问题并查看完整的日志

## 📞 获取帮助

遇到问题？可以通过以下方式获取帮助：

1. 查看文档目录中的其他文档
2. 运行 `./verify-test-env.sh` 进行自动诊断
3. 查看 Docker 容器日志
4. 参考故障排查指南

---

**Happy Testing! 🎉**
