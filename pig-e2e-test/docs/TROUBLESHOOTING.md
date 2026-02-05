# E2E 测试环境故障排查指南

## 🔍 常见问题

### 1. MySQL 相关问题

#### 问题: MySQL 容器无法启动

**症状**:
```bash
ERROR: for pig-e2e-mysql  Container "xxx" is unhealthy
```

**可能原因**:
- 端口 33307 已被占用
- 磁盘空间不足
- 数据目录权限问题

**解决方案**:

```bash
# 1. 检查端口占用
lsof -i :33307
# 或
netstat -an | grep 33307

# 2. 清理旧容器和数据
cd pig-e2e-test
./cleanup-test-env.sh

# 3. 检查磁盘空间
df -h

# 4. 查看 MySQL 日志
docker-compose logs mysql

# 5. 重新启动
./start-test-env.sh
```

#### 问题: 数据库初始化脚本未执行

**症状**:
- 连接 MySQL 后发现数据库或表不存在
- 应用启动时报错找不到表

**验证**:
```bash
# 进入 MySQL 容器
docker exec -it pig-e2e-mysql mysql -uroot -proot

# 检查数据库
SHOW DATABASES;

# 应该看到以下数据库:
# - pig
# - pig_order
# - nacos_config
```

**解决方案**:
```bash
# 1. 完全清理并重启
./cleanup-test-env.sh
./start-test-env.sh

# 2. 如果问题依然存在，手动导入数据库
docker exec -i pig-e2e-mysql mysql -uroot -proot < ../db/pig.sql
docker exec -i pig-e2e-mysql mysql -uroot -proot < ../db/pig_config.sql
docker exec -i pig-e2e-mysql mysql -uroot -proot < ../db/pig_order.sql
```

### 2. Kafka 相关问题

#### 问题: Kafka 无法连接

**症状**:
```
org.apache.kafka.common.errors.TimeoutException: Timeout expired while fetching topic metadata
```

**解决方案**:

```bash
# 1. 检查 Kafka 容器状态
docker ps | grep kafka

# 2. 检查 Kafka 健康状态
docker exec pig-e2e-kafka kafka-broker-api-versions --bootstrap-server localhost:9093

# 3. 查看 Kafka 日志
docker-compose logs kafka

# 4. 验证 Zookeeper
docker exec pig-e2e-zookeeper bash -c "echo ruok | nc localhost 2181"
# 应该返回 "imok"

# 5. 重启 Kafka 相关服务
docker-compose restart zookeeper kafka
```

#### 问题: Kafka 主题未创建

**症状**:
- 应用报错找不到主题
- 消息无法发送
- 出现 `LEADER_NOT_AVAILABLE` 警告

**验证**:
```bash
# 列出所有主题
docker exec pig-e2e-kafka kafka-topics --list --bootstrap-server localhost:9093

# 应该看到:
# - domain.order
# - domain.vault
# - domain.market
```

**解决方案**:

⚠️ **注意**: 从最新版本开始，Kafka 会在启动时自动创建所有必要的主题。如果主题缺失，说明 Kafka 启动脚本未正确执行。

```bash
# 1. 查看 Kafka 容器日志，检查主题创建过程
docker-compose logs kafka | grep -i "creating topics"

# 2. 检查 Kafka 是否完全就绪（healthcheck 会验证所有主题）
docker-compose ps kafka
# 状态应该是 "healthy"

# 3. 如果主题仍然缺失，手动创建
docker exec pig-e2e-kafka kafka-topics --create \
  --if-not-exists \
  --bootstrap-server localhost:9093 \
  --topic domain.order \
  --partitions 3 \
  --replication-factor 1

docker exec pig-e2e-kafka kafka-topics --create \
  --if-not-exists \
  --bootstrap-server localhost:9093 \
  --topic domain.vault \
  --partitions 3 \
  --replication-factor 1

docker exec pig-e2e-kafka kafka-topics --create \
  --if-not-exists \
  --bootstrap-server localhost:9093 \
  --topic domain.market \
  --partitions 3 \
  --replication-factor 1

# 4. 如果问题持续，重启 Kafka
docker-compose restart kafka
```

详情请参阅: [KAFKA-TOPICS-AUTO-CREATE.md](../KAFKA-TOPICS-AUTO-CREATE.md)

### 3. Nacos 相关问题

#### 问题: Nacos 控制台无法访问

**症状**:
- 浏览器访问 http://localhost:8849/nacos 超时或拒绝连接

**解决方案**:

```bash
# 1. 检查 Nacos 容器状态
docker ps | grep nacos

# 2. 查看 Nacos 日志
docker-compose logs nacos

# 3. 检查健康状态
curl http://localhost:8849/nacos/v1/console/health/readiness

# 4. 等待更长时间
# Nacos 启动可能需要 1-2 分钟，耐心等待

# 5. 检查 MySQL 连接
# Nacos 依赖 MySQL，确保 MySQL 已正常启动
docker exec pig-e2e-nacos curl -f http://pig-e2e-mysql:3306
```

#### 问题: Nacos 配置未初始化

**症状**:
- 登录 Nacos 后看不到命名空间或配置

**验证**:
```bash
# 查看配置列表
curl "http://localhost:8849/nacos/v1/cs/configs?tenant=dev&group=DEFAULT_GROUP"

# 查看命名空间
curl "http://localhost:8849/nacos/v1/console/namespaces"
```

**解决方案**:

```bash
# 1. 查看 nacos-init 日志
docker-compose logs nacos-init

# 2. 手动创建命名空间
curl -X POST 'http://localhost:8849/nacos/v1/console/namespaces' \
  -d 'customNamespaceId=dev&namespaceName=dev&namespaceDesc=Development Environment'

# 3. 手动创建配置
# 通过 Nacos 控制台 (http://localhost:8849/nacos) 手动添加
# 或参考 nacos-config-example.yml 文件
```

### 4. Redis 相关问题

#### 问题: Redis 连接失败

**症状**:
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:36380
```

**解决方案**:

```bash
# 1. 检查 Redis 容器状态
docker ps | grep redis

# 2. 测试 Redis 连接
docker exec pig-e2e-redis redis-cli ping
# 应该返回 "PONG"

# 3. 查看 Redis 日志
docker-compose logs redis

# 4. 重启 Redis
docker-compose restart redis
```

### 5. Docker 相关问题

#### 问题: 端口冲突

**症状**:
```
Error starting userland proxy: listen tcp4 0.0.0.0:33307: bind: address already in use
```

**解决方案**:

```bash
# 1. 查找占用端口的进程
lsof -i :33307
# 或
netstat -tulpn | grep 33307

# 2. 停止占用端口的进程
kill -9 <PID>

# 3. 或修改 docker-compose.yml 中的端口映射
# 例如将 33307:3306 改为 33308:3306
```

#### 问题: 磁盘空间不足

**症状**:
```
Error response from daemon: no space left on device
```

**解决方案**:

```bash
# 1. 检查磁盘空间
df -h

# 2. 清理 Docker 悬空镜像
docker image prune -a

# 3. 清理未使用的卷
docker volume prune

# 4. 清理未使用的容器
docker container prune

# 5. 清理构建缓存
docker builder prune
```

#### 问题: 网络问题

**症状**:
- 容器之间无法通信
- 应用无法连接到数据库

**解决方案**:

```bash
# 1. 检查网络
docker network ls | grep pig-e2e

# 2. 检查容器网络连接
docker network inspect pig-e2e-network

# 3. 重新创建网络
docker-compose down
docker network rm pig-e2e-network
docker-compose up -d

# 4. 测试容器间连通性
docker exec pig-e2e-nacos ping pig-e2e-mysql
```

### 6. 性能问题

#### 问题: 容器启动很慢

**可能原因**:
- 系统资源不足
- Docker 配置问题
- 镜像下载慢

**优化方案**:

```bash
# 1. 增加 Docker 资源限制
# Docker Desktop -> Preferences -> Resources
# 建议: CPU: 4 核, 内存: 8GB

# 2. 使用国内镜像源
# 编辑 /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://registry.docker-cn.com",
    "https://docker.mirrors.ustc.edu.cn"
  ]
}

# 3. 预先拉取镜像
docker-compose pull

# 4. 减少服务数量
# 只启动必需的服务，注释掉不需要的服务
```

## 🔨 调试技巧

### 查看所有服务状态

```bash
docker-compose ps
```

### 查看服务日志

```bash
# 实时查看所有日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f kafka
docker-compose logs -f nacos

# 查看最近 100 行日志
docker-compose logs --tail=100
```

### 进入容器内部

```bash
# MySQL
docker exec -it pig-e2e-mysql bash
mysql -uroot -proot

# Redis
docker exec -it pig-e2e-redis sh
redis-cli

# Kafka
docker exec -it pig-e2e-kafka bash
kafka-topics --list --bootstrap-server localhost:9093

# Nacos
docker exec -it pig-e2e-nacos bash
```

### 检查容器资源使用

```bash
# 查看所有容器资源使用
docker stats

# 查看特定容器
docker stats pig-e2e-mysql pig-e2e-kafka
```

### 网络调试

```bash
# 检查容器 IP
docker inspect pig-e2e-mysql | grep IPAddress

# 测试容器间连通性
docker exec pig-e2e-nacos ping pig-e2e-mysql

# 测试端口连通性
docker exec pig-e2e-nacos telnet pig-e2e-mysql 3306
```

## 📝 日志位置

| 服务 | 容器内日志路径 |
|------|---------------|
| MySQL | `/var/log/mysql/` |
| Redis | `/data/` |
| Kafka | `/var/lib/kafka/data/` |
| Nacos | `/home/nacos/logs/` |

## 🆘 获取帮助

如果以上方法都无法解决问题，请：

1. **收集诊断信息**:
   ```bash
   # 创建诊断报告
   cd pig-e2e-test
   docker-compose logs > diagnostic-logs.txt
   docker-compose ps >> diagnostic-logs.txt
   docker network inspect pig-e2e-network >> diagnostic-logs.txt
   ```

2. **检查版本兼容性**:
   ```bash
   docker --version
   docker-compose --version
   java -version
   mvn -version
   ```

3. **查看项目文档**:
   - [README-TEST-ENV.md](README-TEST-ENV.md) - 环境文档
   - [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) - 项目总览

4. **完全重置**:
   ```bash
   # 停止所有容器
   docker-compose down -v
   
   # 删除相关镜像
   docker images | grep pig-e2e | awk '{print $3}' | xargs docker rmi -f
   
   # 清理网络
   docker network prune
   
   # 重新启动
   ./start-test-env.sh
   ```

## 📊 健康检查清单

在报告问题前，请确认以下检查：

- [ ] Docker 服务正常运行
- [ ] 磁盘空间充足（至少 10GB）
- [ ] 内存充足（至少 8GB）
- [ ] 端口未被占用（33307, 36380, 9093, 8849）
- [ ] 防火墙未阻止 Docker 网络
- [ ] Docker Compose 版本 >= 1.27
- [ ] 所有数据库文件存在（../db/ 目录）
- [ ] 已执行完整的清理和重启流程
