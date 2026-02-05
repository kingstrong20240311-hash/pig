# Nacos 配置管理示例

## 场景 1: 修改网关限流配置

### 步骤 1: 查看当前配置

```bash
python scripts/nacos_config.py get \
  --data-id "pig-gateway-dev.yml" \
  --group "DEFAULT_GROUP"
```

### 步骤 2: 准备新配置

创建临时文件 `gateway-config.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
spring:
  cloud:
    gateway:
      routes:
        - id: pig-upms-biz
          uri: lb://pig-upms-biz
          predicates:
            - Path=/admin/**
          filters:
            # 更新限流配置
            - name: RequestRateLimiter
              args:
                key-resolver: '#{@remoteAddrKeyResolver}'
                redis-rate-limiter.replenishRate: 200  # 从 100 改为 200
                redis-rate-limiter.burstCapacity: 400  # 从 200 改为 400
```

### 步骤 3: 更新到 Nacos

```bash
python scripts/nacos_config.py update \
  --data-id "pig-gateway-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "gateway-config.yml" \
  --desc "提高限流阈值"
```

### 步骤 4: 询问用户是否同步

使用 AskQuestion 工具询问用户。

### 步骤 5: 同步到 SQL

```bash
python scripts/sync_to_sql.py \
  --data-id "pig-gateway-dev.yml" \
  --group "DEFAULT_GROUP"
```

## 场景 2: 修改 Redis 连接配置

### 步骤 1: 获取当前配置

```bash
python scripts/nacos_config.py get \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" > current-config.yml
```

### 步骤 2: 编辑配置文件

修改 `current-config.yml` 中的 Redis 配置:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:192.168.1.100}  # 修改主机
      password: ${REDIS_PASSWORD:newpassword}  # 添加密码
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
```

### 步骤 3: 更新配置

```bash
python scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "current-config.yml" \
  --desc "更新 Redis 连接信息"
```

### 步骤 4: 同步到 SQL

```bash
python scripts/sync_to_sql.py \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"
```

## 场景 3: 批量修改数据库连接

### 需求

将所有服务的 MySQL 端口从 3306 改为 33307。

### 步骤 1: 列出所有配置

```bash
python scripts/nacos_config.py list
```

### 步骤 2: 逐个修改

对于每个包含数据库配置的文件(如 `pig-upms-biz-dev.yml`, `pig-order-biz-dev.yml` 等):

```bash
# 获取配置
python scripts/nacos_config.py get \
  --data-id "pig-upms-biz-dev.yml" \
  --group "DEFAULT_GROUP" > upms-config.yml

# 编辑 upms-config.yml,修改 MYSQL_PORT

# 更新配置
python scripts/nacos_config.py update \
  --data-id "pig-upms-biz-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "upms-config.yml" \
  --desc "更新 MySQL 端口为 33307"
```

### 步骤 3: 批量同步到 SQL

```bash
python scripts/sync_to_sql.py --all
```

## 场景 4: 添加新的 Kafka 配置

### 步骤 1: 获取基础配置

```bash
python scripts/nacos_config.py get \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" > app-config.yml
```

### 步骤 2: 添加 Kafka 配置

在 `app-config.yml` 中添加:

```yaml
pig:
  outbox:
    mode: microservice
    kafka:
      bootstrap-servers: "127.0.0.1:9093"
      acks: all
      retries: 3
      max-in-flight-requests-per-connection: 5
      enable-idempotence: true
    consumer:
      enabled: true
      bootstrap-servers: "127.0.0.1:9093"
      domains: ["order", "vault", "market"]
      auto-offset-reset: "earliest"
      enable-auto-commit: false
      max-poll-records: 100
      concurrency: 3
```

### 步骤 3: 更新配置

```bash
python scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "app-config.yml" \
  --desc "添加 Kafka 事件总线配置"
```

### 步骤 4: 同步到 SQL

```bash
python scripts/sync_to_sql.py \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"
```

## 场景 5: 完整导出当前配置

### 用途

定期备份 Nacos 配置到 SQL 文件。

### 命令

```bash
python scripts/sync_to_sql.py --export
```

这会生成类似 `db/pig_config_export_20260129_143022.sql` 的文件。

### 自定义输出路径

```bash
python scripts/sync_to_sql.py --export --output "backups/nacos-$(date +%Y%m%d).sql"
```

## 场景 6: 恢复配置

### 从 SQL 恢复到 Nacos

如果需要从 `db/pig_config.sql` 恢复配置到 Nacos:

1. 导入 SQL 到 MySQL:

```bash
mysql -h127.0.0.1 -P33307 -uroot -proot pig_config < db/pig_config.sql
```

2. 重启 Nacos 服务,配置会自动加载。

或者手动通过脚本导入:

```bash
# 从 SQL 解析配置并推送到 Nacos
# (需要编写额外的脚本实现此功能)
```

## 环境变量配置

### 开发环境

```bash
export NACOS_HOST=127.0.0.1
export NACOS_PORT=8848
export NACOS_NAMESPACE=public
```

### 生产环境

```bash
export NACOS_HOST=nacos.production.com
export NACOS_PORT=8848
export NACOS_NAMESPACE=production
```

## 常见问题

### Q: 修改配置后服务没有生效?

A: 需要重启相关服务或等待配置自动刷新(如果配置了 `@RefreshScope`)。

### Q: 同步失败,SQL 文件格式错误?

A: 确保 SQL 文件是标准的 MySQL dump 格式。脚本会自动备份,可以从 `.bak` 文件恢复。

### Q: 如何验证配置已正确同步?

A: 可以对比 Nacos 和 SQL 文件中的 MD5 值:

```bash
# Nacos 中的配置
python scripts/nacos_config.py get --data-id "xxx" --group "DEFAULT_GROUP"

# SQL 文件中的 MD5
grep "xxx" db/pig_config.sql
```

### Q: 能否同步到不同的命名空间?

A: 可以,使用 `--namespace` 参数:

```bash
python scripts/nacos_config.py list --namespace dev
python scripts/sync_to_sql.py --all --namespace dev
```

---

# 服务管理场景

## 场景 7: 查看所有服务状态

### 需求

查看当前 Nacos 中注册的所有服务及其健康状态。

### 步骤 1: 列出所有服务

```bash
python scripts/nacos_service.py --host 127.0.0.1 --port 18849 list
```

输出示例:
```
总服务数: 5
当前页: 1

服务名称: pig-gateway
------------------------------------------------------------
服务名称: pig-auth
------------------------------------------------------------
服务名称: pig-upms-biz
------------------------------------------------------------
服务名称: pig-order-biz
------------------------------------------------------------
服务名称: pig-vault-biz
------------------------------------------------------------
```

### 步骤 2: 查看详细实例信息

```bash
python scripts/nacos_service.py --host 127.0.0.1 --port 18849 list --with-instances
```

输出示例:
```
总服务数: 5
当前页: 1

服务名称: pig-gateway
  实例数: 1
  健康实例: 1/1
------------------------------------------------------------
服务名称: pig-auth
  实例数: 1
  健康实例: 0/1
------------------------------------------------------------
```

## 场景 8: 服务实例故障排查

### 问题

发现 `pig-order-biz` 服务响应缓慢,需要检查实例状态。

### 步骤 1: 查看服务实例

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  get --service-name "pig-order-biz"
```

输出示例:
```
服务名称: pig-order-biz
分组: DEFAULT_GROUP
命名空间: public
------------------------------------------------------------

实例列表 (共 2 个):

  [1] 192.168.0.201:4000
      状态: ✓ 健康
      启用: 启用
      权重: 1.0
      元数据: {"version": "1.0.0"}

  [2] 192.168.0.202:4000
      状态: ✗ 不健康
      启用: 启用
      权重: 1.0
      元数据: {"version": "1.0.0"}
```

### 步骤 2: 下线不健康实例

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  deregister --service-name "pig-order-biz" \
  --ip "192.168.0.202" --port 4000
```

### 步骤 3: 验证服务恢复

```bash
# 再次查看实例列表
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  instances --service-name "pig-order-biz" --healthy-only
```

## 场景 9: 灰度发布流程

### 需求

对 `pig-gateway` 服务进行灰度发布,先部署新版本,验证无误后切换流量。

### 步骤 1: 查看当前实例

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  instances --service-name "pig-gateway"
```

当前运行:
- 旧版本: 192.168.0.200:9999

### 步骤 2: 标记旧实例为不健康

暂时将流量从旧实例切走:

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  health --service-name "pig-gateway" \
  --ip "192.168.0.200" --port 9999 \
  --healthy false
```

### 步骤 3: 部署新版本实例

启动新版本服务(192.168.0.210:9999),它会自动注册到 Nacos。

### 步骤 4: 验证新实例

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  instances --service-name "pig-gateway"
```

应该看到:
- 旧版本: 192.168.0.200:9999 (不健康)
- 新版本: 192.168.0.210:9999 (健康)

### 步骤 5: 完全切换流量

验证新版本运行正常后,下线旧实例:

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  deregister --service-name "pig-gateway" \
  --ip "192.168.0.200" --port 9999 --force
```

## 场景 10: 配置更新后验证服务

### 需求

更新配置后,检查相关服务是否正常运行。

### 步骤 1: 更新配置

```bash
python scripts/nacos_config.py \
  --host 127.0.0.1 --port 18849 \
  update --data-id "application-dev.yml" \
  --file "new-config.yml" \
  --desc "更新 Redis 配置"
```

### 步骤 2: 同步到 SQL

```bash
python scripts/sync_to_sql.py \
  --host 127.0.0.1 --port 18849 \
  --data-id "application-dev.yml"
```

### 步骤 3: 检查所有服务状态

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  list --with-instances
```

### 步骤 4: 检查特定服务

如果发现某个服务异常:

```bash
python scripts/nacos_service.py \
  --host 127.0.0.1 --port 18849 \
  get --service-name "pig-upms-biz"
```

## 场景 11: 定期健康检查脚本

### 需求

创建脚本定期检查所有服务的健康状态。

### 脚本: check_services.sh

```bash
#!/bin/bash
# 服务健康检查脚本

NACOS_HOST="127.0.0.1"
NACOS_PORT="18849"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Nacos 服务健康检查"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""

# 列出所有服务及实例
python3 "$SCRIPT_DIR/scripts/nacos_service.py" \
  --host "$NACOS_HOST" \
  --port "$NACOS_PORT" \
  list --with-instances

echo ""
echo "========================================="
echo "检查完成"
echo "========================================="
```

### 使用

```bash
chmod +x check_services.sh
./check_services.sh
```

### 定时任务

添加到 crontab,每 5 分钟检查一次:

```bash
*/5 * * * * /path/to/check_services.sh >> /var/log/nacos-health-check.log 2>&1
```
