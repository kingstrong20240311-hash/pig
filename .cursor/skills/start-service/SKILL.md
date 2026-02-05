---
name: start-service
description: 使用 start-service.sh 脚本启动微服务，并在 logs/ 目录查看日志。当用户需要启动服务、查看日志、重启服务或询问服务启动方式时使用。
---

# 服务启动和日志管理

## 启动服务

使用项目根目录的 `start-service.sh` 脚本启动微服务：

```bash
# 启动默认服务（pig-order/pig-order-biz）
./start-service.sh

# 启动特定服务
./start-service.sh pig-gateway
./start-service.sh pig-vault/pig-vault-biz
./start-service.sh pig-upms/pig-upms-biz
```

## 脚本功能

- **自动清理**：杀掉已存在的同名 Java 进程
- **日志清理**：删除旧的 debug.log 和 error.log
- **环境配置**：自动设置 JVM 参数和环境变量
- **Maven 启动**：使用 `mvn spring-boot:run` 启动服务

## 日志位置

所有服务的运行日志位于 `logs/` 目录：

```
logs/
  order-biz/          # 订单服务日志目录
    debug.log         # 调试日志
    error.log         # 错误日志
  gateway/            # 网关服务日志目录
  vault-biz/          # 金库服务日志目录
  upms-biz/           # 用户权限管理日志目录
  ...
```

## 查看日志

```bash
# 实时查看特定服务日志
tail -f logs/order-biz/debug.log

# 查看错误日志
tail -f logs/order-biz/error.log

# 查看所有日志
tail -f logs/*/*.log

# 搜索关键词
grep "ERROR" logs/order-biz/debug.log
```

## 常见任务

### 重启服务

```bash
# 脚本会自动杀掉旧进程，直接重新运行即可
./start-service.sh pig-order/pig-order-biz
```

### 修改 JVM 参数

编辑脚本中的 `JAVA_TOOL_OPTIONS`：

```bash
export JAVA_TOOL_OPTIONS="-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC"
```

## 注意事项

- 必须在项目根目录执行脚本
- 服务启动需要时间，观察日志确认启动成功
