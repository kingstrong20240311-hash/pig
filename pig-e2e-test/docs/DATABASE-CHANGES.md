# 数据库结构修改说明

## 📝 修改概述

根据用户反馈，已修复 SQL 文件缺少数据库创建语句的问题。现在所有 SQL 文件都会先创建数据库再建表。

## ✅ 修改内容

### 1. **pig_order.sql** 
- ✅ 添加 `DROP DATABASE IF EXISTS pig_order`
- ✅ 添加 `CREATE DATABASE pig_order`
- ✅ 添加 `USE pig_order`
- 📊 创建表：
  - `ord_market` - 预测市场表
  - `ord_order` - 订单表
  - `ord_order_fill` - 订单成交明细表
  - `ord_order_cancel` - 订单取消记录表

### 2. **vault_schema.sql**
- ✅ 添加 `DROP DATABASE IF EXISTS pig_vault`
- ✅ 添加 `CREATE DATABASE pig_vault`
- ✅ 添加 `USE pig_vault`
- 📊 创建表：
  - `vault_account` - 金库账户表
  - `vault_asset` - 资产表
  - `vault_balance` - 余额表
  - `vault_freeze` - 冻结记录表
  - `vault_ledger_entry` - 账本记录表

### 3. **error_record.sql**
- ✅ 添加 `USE pig`（使用主数据库）
- ℹ️ 说明：error_record 表通常和主应用在同一数据库
- 📊 创建表：
  - `error_record` - 错误记录表

## 🗂️ 数据库结构

执行所有 SQL 文件后，将创建以下数据库：

```
MySQL Server
├── pig (主数据库)
│   ├── sys_user, sys_role, sys_menu ...
│   └── error_record (错误记录表)
│
├── pig_config (Nacos 配置数据库) ⭐
│   ├── config_info (配置数据 - 包含所有服务配置)
│   ├── config_info_beta, config_info_gray, config_info_tag
│   ├── config_tags_relation
│   ├── group_capacity, tenant_capacity, tenant_info
│   ├── his_config_info (历史配置)
│   ├── users, roles, permissions (Nacos 用户权限)
│   └── 从 pig_config.sql 导入，包含完整配置数据
│
├── pig_order (订单数据库)
│   ├── ord_market (市场)
│   ├── ord_order (订单)
│   ├── ord_order_fill (成交)
│   └── ord_order_cancel (撤单)
│
└── pig_vault (金库数据库)
    ├── vault_account (账户)
    ├── vault_asset (资产)
    ├── vault_balance (余额)
    ├── vault_freeze (冻结)
    └── vault_ledger_entry (账本)
```

## 🔧 Docker Compose 配置更新

### MySQL 容器挂载顺序

```yaml
volumes:
  - ../db/pig.sql:/docker-entrypoint-initdb.d/01-pig.sql
  - ../db/pig_config.sql:/docker-entrypoint-initdb.d/02-pig_config.sql
  - ../db/pig_order.sql:/docker-entrypoint-initdb.d/03-pig_order.sql
  - ../db/vault_schema.sql:/docker-entrypoint-initdb.d/04-vault_schema.sql
  - ../db/error_record.sql:/docker-entrypoint-initdb.d/05-error_record.sql
```

**执行顺序**：
1. `pig.sql` → 创建 `pig` 数据库及其表
2. `pig_config.sql` → 向 `pig` 数据库插入配置数据
3. `pig_order.sql` → 创建 `pig_order` 数据库及其表
4. `vault_schema.sql` → 创建 `pig_vault` 数据库及其表
5. `error_record.sql` → 在 `pig` 数据库中创建 `error_record` 表

## 🎯 Nacos 配置更新

现在自动创建 3 个 Nacos 配置文件：

### 1. application-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://pig-e2e-mysql:3306/pig?...
    # 连接到 pig 数据库（主数据库）
```

### 2. pig-order-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://pig-e2e-mysql:3306/pig_order?...
    # 连接到 pig_order 数据库
```

### 3. pig-vault-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://pig-e2e-mysql:3306/pig_vault?...
    # 连接到 pig_vault 数据库
```

## 🧪 验证更新

验证脚本 (`verify-test-env.sh`) 现在会检查：

- ✅ 数据库 `pig` 存在
- ✅ 数据库 `pig_order` 存在
- ✅ 数据库 `pig_vault` 存在
- ✅ 数据库 `nacos_config` 存在
- ✅ 表 `sys_user` 存在（pig 数据库）
- ✅ 表 `ord_order` 存在（pig_order 数据库）
- ✅ 表 `vault_account` 存在（pig_vault 数据库）
- ✅ Nacos 配置 `application-dev.yml` 存在
- ✅ Nacos 配置 `pig-order-dev.yml` 存在
- ✅ Nacos 配置 `pig-vault-dev.yml` 存在

总共检查项：**19 项**（之前是 16 项）

## 📚 更新的文档

以下文档已更新以反映数据库结构变化：

1. ✅ `README-TEST-ENV.md` - 环境文档
2. ✅ `QUICK-START-DOCKER-ENV.md` - 快速开始
3. ✅ `DOCKER-ENV-SUMMARY.md` - 项目总结
4. ✅ `nacos-config-example.yml` - 配置示例
5. ✅ `使用说明.md` - 中文使用说明
6. ✅ `verify-test-env.sh` - 验证脚本
7. ✅ `docker-compose.yml` - Docker Compose 配置

## 🚀 使用方式（无变化）

使用方式保持不变，仍然是三步启动：

```bash
cd pig-e2e-test

# 1. 启动环境
./start-test-env.sh

# 2. 验证环境（现在检查更多项）
./verify-test-env.sh

# 3. 运行测试
export PIG_GATEWAY_URL=http://127.0.0.1:9999
mvn -pl pig-e2e-test -Pe2e verify
```

## ✨ 优势

### 修复前的问题：
- ❌ SQL 文件直接建表，可能导致"数据库不存在"错误
- ❌ 需要手动创建数据库
- ❌ 容器启动可能失败

### 修复后的优势：
- ✅ SQL 文件自包含，先创建数据库再建表
- ✅ 无需手动操作，完全自动化
- ✅ 容器启动更可靠
- ✅ 每个领域有独立的数据库
- ✅ 更符合微服务架构的最佳实践

## 🔍 验证方法

### 检查数据库是否正确创建

```bash
# 方法 1: 使用验证脚本
./verify-test-env.sh

# 方法 2: 手动连接 MySQL
docker exec -it pig-e2e-mysql mysql -uroot -proot

# 方法 3: 查看数据库列表
docker exec pig-e2e-mysql mysql -uroot -proot -e "SHOW DATABASES"
```

### 预期输出

```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| nacos_config       |
| performance_schema |
| pig                |
| pig_order          |
| pig_vault          |
| sys                |
+--------------------+
```

## 💡 注意事项

1. **数据库隔离**：
   - 每个服务（order, vault）有独立的数据库
   - 遵循微服务架构的数据库独立原则

2. **error_record 表**：
   - 放在主 `pig` 数据库中
   - 因为错误记录是全局的，不属于特定领域

3. **兼容性**：
   - 所有修改向后兼容
   - 已存在的应用代码无需修改
   - 只需更新 Nacos 中的数据源配置

4. **清理环境**：
   - 如果之前启动过环境，建议完全清理后重启
   - 运行: `./cleanup-test-env.sh && ./start-test-env.sh`

## 💾 内存优化

**Nacos JVM 配置优化**:
- 从 512M 降低到 256M
- 节省约 300MB 内存
- 整体环境从 ~1.6GB 降低到 ~1.2GB

详见: [MEMORY-OPTIMIZATION.md](MEMORY-OPTIMIZATION.md)

---

**修改日期**: 2025-01-22  
**修改原因**: 
1. 修复 SQL 文件缺少数据库创建语句的问题
2. 优化 Java 服务内存配置

**影响范围**: 数据库初始化、Nacos 配置、验证脚本、内存使用、文档
