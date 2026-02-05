---
name: manage-nacos-config
description: 通过 Nacos Open API 管理配置中心和服务注册。支持配置管理(修改配置、同步到 db/pig_config.sql)和服务管理(查看服务、实例健康状态、下线实例)。当用户需要修改 Nacos 配置、管理服务实例、或查看服务健康状态时使用。
---

# Nacos 配置与服务管理

## 快速开始

本技能帮助你通过 Nacos Open API:
1. **配置管理**: 修改配置中心的配置,并可选择将修改同步到 `db/pig_config.sql` 备份文件
2. **服务管理**: 查看注册的服务、实例健康状态,管理服务实例

## 使用场景

### 配置管理
- 修改 Nacos 配置中心的配置项
- 批量更新多个服务的配置
- 将 Nacos 配置导出备份到 SQL 文件
- 保持配置中心与 SQL 备份文件同步

### 服务管理
- 查看所有注册的服务
- 查看服务实例列表和健康状态
- 下线/上线服务实例
- 监控服务实例健康状况

## 工作流程

### 1. 修改 Nacos 配置

使用辅助脚本操作 Nacos API:

```bash
# 列出所有配置
python scripts/nacos_config.py list

# 获取特定配置
python scripts/nacos_config.py get \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"

# 更新配置
python scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --content "配置内容" \
  --desc "配置说明"

# 从文件更新配置
python scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "/path/to/config.yml" \
  --desc "配置说明"
```

### 2. 询问用户是否同步到 SQL

**重要**: 每次修改 Nacos 配置后,必须询问用户是否需要同步到 `db/pig_config.sql`。

使用 AskQuestion 工具询问:

```
是否需要将修改同步到 db/pig_config.sql 备份文件?
选项:
- 是 - 同步到 SQL 文件
- 否 - 仅更新 Nacos
```

### 3. 同步到 pig_config.sql

如果用户选择同步,使用同步脚本:

```bash
# 同步单个配置
python scripts/sync_to_sql.py \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"

# 同步所有配置
python scripts/sync_to_sql.py --all

# 生成新的 SQL 文件(完整导出)
python scripts/sync_to_sql.py --export
```

## 脚本说明

### nacos_config.py

与 Nacos Open API 交互的主脚本。

**环境变量配置**:
```bash
NACOS_HOST=127.0.0.1
NACOS_PORT=8848
NACOS_NAMESPACE=public  # 可选,默认 public
```

**支持操作**:
- `list`: 列出所有配置
- `get`: 获取配置详情
- `update`: 更新配置
- `delete`: 删除配置

### sync_to_sql.py

将 Nacos 配置同步到 `db/pig_config.sql` 文件。

**功能**:
- 从 Nacos 获取最新配置
- 更新 SQL 文件中对应的 INSERT 语句
- 保持 SQL 文件格式不变
- 自动计算 MD5 值
- 更新修改时间戳

## 配置项说明

### Nacos 配置结构

每个配置包含以下字段:
- `data_id`: 配置 ID(如 `application-dev.yml`)
- `group_id`: 配置分组(通常是 `DEFAULT_GROUP`)
- `content`: 配置内容(YAML 格式)
- `tenant_id`: 租户 ID(命名空间)
- `type`: 配置类型(`yaml`)
- `c_desc`: 配置描述

### pig_config.sql 格式

SQL 文件包含 `config_info` 表的 INSERT 语句,每个配置是一个元组:

```sql
INSERT INTO `config_info` VALUES 
(id, 'data_id', 'group_id', 'content', 'md5', 
'create_time', 'modify_time', 'src_user', 'src_ip', 
'app_name', 'tenant_id', 'c_desc', 'c_use', 'effect', 
'type', 'c_schema', 'encrypted_data_key');
```

## 完整示例

```bash
# 1. 查看当前配置
python scripts/nacos_config.py get \
  --data-id "pig-gateway-dev.yml" \
  --group "DEFAULT_GROUP"

# 2. 修改配置
python scripts/nacos_config.py update \
  --data-id "pig-gateway-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "configs/gateway-updated.yml" \
  --desc "更新网关限流配置"

# 3. 询问用户是否同步 [使用 AskQuestion]

# 4. 如果用户确认,同步到 SQL
python scripts/sync_to_sql.py \
  --data-id "pig-gateway-dev.yml" \
  --group "DEFAULT_GROUP"
```

## 注意事项

1. **必须询问用户**: 修改 Nacos 配置后,始终询问用户是否同步到 SQL
2. **备份**: 同步前自动创建 `db/pig_config.sql.bak` 备份
3. **格式保持**: 同步时保持 SQL 文件的原始格式和结构
4. **MD5 计算**: 自动计算配置内容的 MD5 值
5. **时间戳**: 自动更新 `gmt_modified` 字段为当前时间
6. **编码转义**: 正确处理 SQL 字符串中的特殊字符(引号、反斜杠等)

## 错误处理

脚本会处理常见错误:
- Nacos 连接失败
- 配置不存在
- SQL 文件格式错误
- 权限问题

所有错误会输出清晰的错误信息和建议的解决方案。

## 服务管理

### 列出所有服务

```bash
# 列出所有注册的服务
python scripts/nacos_service.py list

# 显示实例统计信息
python scripts/nacos_service.py list --with-instances
```

### 查看服务详情

```bash
# 查看服务的所有实例
python scripts/nacos_service.py get --service-name "pig-gateway"

# 只查看实例列表
python scripts/nacos_service.py instances --service-name "pig-gateway"

# 只显示健康实例
python scripts/nacos_service.py instances \
  --service-name "pig-gateway" \
  --healthy-only
```

### 管理服务实例

```bash
# 标记实例为不健康
python scripts/nacos_service.py health \
  --service-name "pig-gateway" \
  --ip "192.168.0.200" \
  --port 9999 \
  --healthy false

# 恢复实例健康状态
python scripts/nacos_service.py health \
  --service-name "pig-gateway" \
  --ip "192.168.0.200" \
  --port 9999 \
  --healthy true

# 下线服务实例
python scripts/nacos_service.py deregister \
  --service-name "pig-gateway" \
  --ip "192.168.0.200" \
  --port 9999
```

## 常见工作流

### 场景 1: 更新配置并检查服务

```bash
# 1. 修改配置
python scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --file "new-config.yml"

# 2. 询问用户是否同步 [使用 AskQuestion]

# 3. 同步到 SQL
python scripts/sync_to_sql.py --data-id "application-dev.yml"

# 4. 检查相关服务状态
python scripts/nacos_service.py list --with-instances

# 5. 查看具体服务实例
python scripts/nacos_service.py get --service-name "pig-gateway"
```

### 场景 2: 服务故障排查

```bash
# 1. 列出所有服务及实例状态
python scripts/nacos_service.py list --with-instances

# 2. 查看问题服务的详情
python scripts/nacos_service.py instances --service-name "pig-order-biz"

# 3. 如果有不健康实例,可以手动下线
python scripts/nacos_service.py deregister \
  --service-name "pig-order-biz" \
  --ip "192.168.0.201" \
  --port 4000
```

### 场景 3: 灰度发布准备

```bash
# 1. 查看当前服务实例
python scripts/nacos_service.py instances --service-name "pig-gateway"

# 2. 标记旧版本实例为不健康(流量切走)
python scripts/nacos_service.py health \
  --service-name "pig-gateway" \
  --ip "192.168.0.200" \
  --port 9999 \
  --healthy false

# 3. 部署新版本实例后,下线旧实例
python scripts/nacos_service.py deregister \
  --service-name "pig-gateway" \
  --ip "192.168.0.200" \
  --port 9999
```

## 依赖项

Python 包(scripts 目录下的脚本会自动检查):
- `requests`: HTTP 请求
- `pyyaml`: YAML 解析(仅配置管理需要)

如果缺少依赖,脚本会提示安装命令:
```bash
pip install requests pyyaml
```
