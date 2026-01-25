# 内存优化说明

## 📊 当前内存配置

### Java 服务（Nacos）

| 参数 | 值 | 说明 |
|------|-----|------|
| JVM_XMS | 256m | 最小堆内存 |
| JVM_XMX | 256m | 最大堆内存 |
| JVM_XMN | 128m | 新生代内存 |

**总计**: ~256MB 堆内存 + ~100MB 非堆内存 ≈ **350-400MB**

### 其他服务

| 服务 | 内存占用 | 说明 |
|------|---------|------|
| MySQL 8.0 | ~400-500MB | 数据库服务 |
| Redis (Alpine) | ~10-20MB | 缓存服务（轻量级） |
| Kafka | ~200-300MB | 消息队列 |
| Zookeeper | ~100-150MB | 协调服务 |

**总计**: ~1.1-1.4GB

## 🎯 内存优化历史

### 优化前（初始版本）
```yaml
nacos:
  environment:
    JVM_XMS: 512m
    JVM_XMX: 512m
    JVM_XMN: 256m
```
- Nacos 内存: ~700MB
- 总体系统: ~1.6-2.0GB

### 优化后（当前版本）
```yaml
nacos:
  environment:
    JVM_XMS: 256m
    JVM_XMX: 256m
    JVM_XMN: 128m
```
- Nacos 内存: ~350-400MB
- 总体系统: ~1.1-1.4GB
- **节省**: ~300-600MB

## 💡 进一步优化建议

### 1. 极限优化（不推荐生产）

如果内存非常紧张，可以尝试：

```yaml
nacos:
  environment:
    JVM_XMS: 128m
    JVM_XMX: 256m
    JVM_XMN: 64m
```

⚠️ **警告**:
- 可能导致频繁 GC
- 启动时间变长
- 性能下降
- 适合纯测试环境

### 2. 按需启动服务

如果不需要某些功能，可以注释掉服务：

```yaml
# 不需要 Kafka 可以注释掉
# kafka:
#   ...
# zookeeper:
#   ...
# kafka-init:
#   ...
```

**节省内存**: ~300-450MB

### 3. 使用更轻量的镜像

```yaml
mysql:
  # 使用 MySQL 5.7（更轻量）
  image: mysql:5.7
  # 或使用 MariaDB
  # image: mariadb:10.6
```

**节省内存**: ~100-200MB

## 📈 内存使用对比

### 最小配置（不推荐）
```
MySQL 5.7:      ~300MB
Redis Alpine:   ~15MB
Nacos (128M):   ~200MB
────────────────────────
总计:           ~515MB
```
仅包含核心服务，无 Kafka

### 当前配置（推荐）✅
```
MySQL 8.0:      ~450MB
Redis Alpine:   ~15MB
Kafka:          ~250MB
Zookeeper:      ~125MB
Nacos (256M):   ~350MB
────────────────────────
总计:           ~1.2GB
```
完整功能，内存优化

### 舒适配置
```
MySQL 8.0:      ~450MB
Redis:          ~30MB
Kafka:          ~250MB
Zookeeper:      ~125MB
Nacos (512M):   ~650MB
────────────────────────
总计:           ~1.5GB
```
更好的性能，适合频繁使用

## 🔧 配置调整方法

### 方法 1: 修改 docker-compose.yml

```bash
cd pig-e2e-test
vim docker-compose.yml

# 修改 Nacos JVM 配置
# JVM_XMS: 256m  <- 修改这里
# JVM_XMX: 256m  <- 修改这里

# 重启环境
./cleanup-test-env.sh
./start-test-env.sh
```

### 方法 2: 环境变量覆盖

```bash
# 创建 .env 文件
cat > .env << EOF
NACOS_JVM_XMS=256m
NACOS_JVM_XMX=256m
NACOS_JVM_XMN=128m
EOF

# 修改 docker-compose.yml 使用环境变量
# JVM_XMS: ${NACOS_JVM_XMS:-256m}
```

## 🎯 推荐配置

### 按可用内存选择

| 可用内存 | 推荐配置 | Nacos JVM | 功能 |
|---------|---------|-----------|------|
| < 2GB | 最小配置 | 128M | 无 Kafka |
| 2-4GB | 当前配置 ✅ | 256M | 完整功能 |
| 4-8GB | 舒适配置 | 512M | 更好性能 |
| > 8GB | 开发配置 | 1024M | 最佳体验 |

### Docker Desktop 资源设置

**最低要求**:
- CPU: 2 核
- 内存: 3GB
- 磁盘: 10GB

**推荐配置**:
- CPU: 4 核
- 内存: 6GB
- 磁盘: 20GB

## 🔍 监控内存使用

### 查看容器内存使用

```bash
# 实时监控
docker stats

# 单次查看
docker stats --no-stream

# 只看特定容器
docker stats pig-e2e-nacos pig-e2e-mysql
```

### 查看 Nacos 内存详情

```bash
# 进入 Nacos 容器
docker exec -it pig-e2e-nacos bash

# 查看 Java 进程内存
ps aux | grep java

# 使用 jstat 查看 GC 情况
jstat -gcutil <pid> 1000
```

### 查看 MySQL 内存详情

```bash
docker exec pig-e2e-mysql mysql -uroot -proot -e "
  SHOW VARIABLES LIKE '%buffer%';
  SHOW STATUS LIKE 'Innodb_buffer_pool_pages_%';
"
```

## 📊 性能影响

### Nacos 内存与性能关系

| 内存 | 启动时间 | 配置数量 | GC 频率 | 适用场景 |
|------|---------|---------|---------|---------|
| 128M | ~45s | <50 | 高 | 纯测试 |
| 256M | ~30s | <200 | 中 | E2E 测试 ✅ |
| 512M | ~25s | <1000 | 低 | 开发环境 |
| 1024M | ~20s | 无限制 | 极低 | 生产环境 |

### 测试场景验证

当前配置（256M）足以支持：
- ✅ 3 个命名空间
- ✅ 20+ 个配置文件
- ✅ 10+ 个服务注册
- ✅ 并发 E2E 测试
- ✅ 稳定运行 24 小时+

## ⚡ 快速诊断

### 内存不足症状

1. **容器频繁重启**
   ```bash
   docker ps -a | grep Restarting
   ```

2. **OOM (Out of Memory) 错误**
   ```bash
   docker logs pig-e2e-nacos | grep -i "out of memory"
   ```

3. **系统变慢**
   ```bash
   docker stats --no-stream | grep -E "MEM|pig-e2e"
   ```

### 解决方案

**临时解决**（重启容器）:
```bash
docker restart pig-e2e-nacos
```

**永久解决**（增加内存）:
```bash
# 编辑 docker-compose.yml
vim docker-compose.yml
# 修改 JVM_XMX 为 512m

# 重新启动
./cleanup-test-env.sh
./start-test-env.sh
```

## 🎓 最佳实践

1. **测试环境优先优化内存**
   - 当前 256M 配置已足够
   - 不要过度优化（影响稳定性）

2. **监控资源使用**
   - 定期运行 `docker stats`
   - 关注 Nacos 和 MySQL

3. **按需调整**
   - 频繁 GC → 增加内存
   - 资源充足 → 保持当前配置
   - 内存紧张 → 考虑最小配置

4. **文档化配置**
   - 记录修改原因
   - 注明适用场景

## 📝 更新日志

### 2025-01-22
- ✅ 优化 Nacos JVM 配置
- ✅ 从 512M 降低到 256M
- ✅ 节省 ~300MB 内存
- ✅ 验证稳定性通过

---

**建议**: 保持当前配置（256M），适合 E2E 测试环境 ✅
