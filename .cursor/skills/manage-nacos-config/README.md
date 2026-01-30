# Nacos 配置与服务管理技能

通过 Nacos Open API 管理配置中心和服务注册。

## 功能特性

### 配置管理
- ✅ 修改 Nacos 配置
- ✅ 同步配置到 `db/pig_config.sql`
- ✅ 导出所有配置到 SQL

### 服务管理
- ✅ 查看所有注册服务
- ✅ 查看服务实例列表
- ✅ 查看实例健康状态
- ✅ 下线/上线服务实例

## 快速开始

### 1. 安装依赖

```bash
pip install requests pyyaml
```

### 2. 配置环境变量

```bash
export NACOS_HOST=127.0.0.1
export NACOS_PORT=8848
export NACOS_NAMESPACE=public
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
```

### 3. 配置管理

```bash
# 列出所有配置
python scripts/nacos_config.py list

# 获取配置
python scripts/nacos_config.py get --data-id "application-dev.yml"

# 更新配置
python scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --file "config.yml" \
  --desc "更新说明"

# 同步到 SQL
python scripts/sync_to_sql.py --data-id "application-dev.yml"
```

### 4. 服务管理

```bash
# 列出所有服务
python scripts/nacos_service.py list

# 查看服务实例
python scripts/nacos_service.py get --service-name "pig-gateway"

# 下线实例
python scripts/nacos_service.py deregister \
  --service-name "pig-gateway" \
  --ip "192.168.0.200" \
  --port 9999
```

## 文件说明

- `SKILL.md` - 技能主文档,包含完整的使用说明
- `QUICKSTART.md` - 快速入门指南
- `examples.md` - 实际场景示例
- `scripts/nacos_config.py` - 配置管理工具
- `scripts/nacos_service.py` - 服务管理工具
- `scripts/sync_to_sql.py` - 配置同步到 SQL 工具

## 工作流程

1. 使用 `nacos_config.py` 修改 Nacos 配置
2. **必须询问用户**是否需要同步到 SQL
3. 如果用户确认,使用 `sync_to_sql.py` 同步

## 注意事项

- 每次修改配置后必须询问用户是否同步
- 同步前会自动创建 `.bak` 备份文件
- 所有操作都会保持 SQL 文件格式不变
- 自动计算 MD5 和更新时间戳
