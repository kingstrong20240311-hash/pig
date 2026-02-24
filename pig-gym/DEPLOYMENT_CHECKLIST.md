# Pig-Gym 模块部署检查清单

## ✅ 部署前检查

### 1. 环境准备
- [ ] MySQL 8.0+ 已启动并可访问
- [ ] Redis 已启动并可访问
- [ ] Nacos 8848 端口已启动 (pig-register)
- [ ] Java 21 已安装
- [ ] Maven 3.6+ 已安装

### 2. 项目构建
```bash
# 构建 pig-gym 模块
cd /Users/luka/Desktop/lrepo/polymarket/back
mvn clean package -DskipTests -pl pig-gym/pig-gym-api,pig-gym/pig-gym-biz -am
```

- [ ] 构建成功，无错误
- [ ] JAR 文件已生成：`pig-gym/pig-gym-biz/target/pig-gym-biz.jar`

## 📦 数据库配置

### 1. 创建数据库
```bash
mysql -u root -p < db/pig_gym_schema.sql
```

**验证：**
```sql
USE pig_gym;
SHOW TABLES;
-- 应该看到 6 张表
```

- [ ] pig_gym 数据库已创建
- [ ] 6 张业务表已创建
- [ ] 示例数据已插入

## 🔧 Nacos 配置

### 方式一：自动配置（推荐）

```bash
mysql -u root -p < db/add_pig_gym_config.sql
```

- [ ] SQL 执行成功
- [ ] 配置检查通过

### 方式二：手动配置

登录 Nacos：http://localhost:8848/nacos (nacos/nacos)

**创建配置：**
1. [ ] 配置管理 → 配置列表 → 创建 `pig-gym-biz-dev.yml`
2. [ ] 编辑 `pig-gateway-dev.yml`，添加 gym 路由

**验证：**
- [ ] Nacos 配置列表中存在 `pig-gym-biz-dev.yml`
- [ ] `pig-gateway-dev.yml` 包含 gym 路由配置

## 🚀 服务启动

### 启动顺序

1. **基础服务**
```bash
# 1. Nacos (如未启动)
cd pig-register
mvn spring-boot:run

# 2. Gateway
cd pig-gateway
mvn spring-boot:run

# 3. Auth
cd pig-auth
mvn spring-boot:run
```

2. **Gym 服务**
```bash
cd pig-gym/pig-gym-biz
mvn spring-boot:run
```

### 启动日志检查

- [ ] 看到 "Started PigGymApplication"
- [ ] 没有 ERROR 级别日志
- [ ] Nacos 注册成功
- [ ] 配置加载成功

```
关键日志：
✅ Nacos registry, pig-gym-biz 127.0.0.1:5010 register finished
✅ Located property source: pig-gym-biz-dev.yml
✅ Started PigGymApplication in X.XXX seconds
```

## ✅ 功能验证

### 1. 健康检查

```bash
# 直接访问
curl http://localhost:5010/actuator/health

# 预期返回：
{"status":"UP"}
```

- [ ] 直接访问健康检查成功

### 2. 网关路由测试

```bash
# 通过网关访问
curl http://localhost:9999/gym/actuator/health

# 预期返回：
{"status":"UP"}
```

- [ ] 网关路由正常

### 3. Nacos 注册验证

访问：http://localhost:8848/nacos → 服务管理 → 服务列表

- [ ] 看到 `pig-gym-biz` 服务
- [ ] 健康实例数 = 1
- [ ] 实例详情显示端口 5010

### 4. API 文档访问

**直接访问：**
- 访问：http://localhost:5010/doc.html
- [ ] 能够打开 Swagger 文档
- [ ] 看到 "健身管理" 标签

**通过网关：**
- 访问：http://localhost:9999/gym/doc.html
- [ ] 能够打开 Swagger 文档

### 5. 数据库连接测试

查看服务日志，确认：
- [ ] HikariPool 连接池创建成功
- [ ] 没有数据库连接错误

## 🔍 故障排查

### 问题：服务无法启动

**检查项：**
1. [ ] 端口 5010 是否被占用？
   ```bash
   lsof -i:5010
   ```

2. [ ] Nacos 是否可访问？
   ```bash
   curl http://localhost:8848/nacos
   ```

3. [ ] 数据库 pig_gym 是否存在？
   ```sql
   SHOW DATABASES LIKE 'pig_gym';
   ```

### 问题：服务启动但未注册到 Nacos

**检查项：**
1. [ ] application.yml 中 Nacos 配置正确
2. [ ] Nacos 服务正常运行
3. [ ] 防火墙未阻止 8848 端口

### 问题：无法通过网关访问

**检查项：**
1. [ ] pig-gateway 已启动
2. [ ] pig-gateway-dev.yml 包含 gym 路由
3. [ ] 重启 pig-gateway 使配置生效

### 问题：数据库连接失败

**检查项：**
1. [ ] MySQL 运行正常
2. [ ] 数据库用户名密码正确
3. [ ] pig_gym 数据库已创建
4. [ ] 环境变量配置正确

## 📊 性能检查

### 1. 连接池状态

访问：http://localhost:5010/actuator/metrics/hikaricp.connections

- [ ] active < maximum-pool-size
- [ ] idle > 0

### 2. 内存使用

```bash
# JVM 内存
curl http://localhost:5010/actuator/metrics/jvm.memory.used
```

- [ ] 堆内存使用正常

### 3. 响应时间

```bash
# 测试响应时间
time curl http://localhost:5010/actuator/health
```

- [ ] 响应时间 < 500ms

## 🎯 生产环境额外检查

- [ ] 日志级别改为 INFO
- [ ] 数据库密码已加密
- [ ] 配置了监控告警
- [ ] 配置了限流规则
- [ ] 准备了回滚方案

## 📝 部署记录

| 项目 | 状态 | 备注 | 操作人 | 时间 |
|------|------|------|--------|------|
| 代码构建 | ⬜ |  |  |  |
| 数据库初始化 | ⬜ |  |  |  |
| Nacos 配置 | ⬜ |  |  |  |
| 服务启动 | ⬜ |  |  |  |
| 功能验证 | ⬜ |  |  |  |
| 性能检查 | ⬜ |  |  |  |

## 🆘 紧急联系

- **项目负责人：** [填写]
- **运维负责人：** [填写]
- **DBA：** [填写]

## 📚 相关文档

- [Nacos 配置指南](./NACOS_CONFIG.md)
- [模块说明](./README.md)
- [项目文档](../CLAUDE.md)
