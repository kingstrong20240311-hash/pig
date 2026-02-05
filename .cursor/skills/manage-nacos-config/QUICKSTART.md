# 快速入门指南

## 1. 安装依赖

```bash
pip install requests pyyaml
```

## 2. 设置环境变量(可选)

默认连接本地 Nacos:

```bash
export NACOS_HOST=127.0.0.1
export NACOS_PORT=8848
export NACOS_NAMESPACE=public
```

## 3. 第一次使用

### 查看所有配置

```bash
cd /Users/luka/Desktop/lrepo/polymarket/back
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py list
```

### 获取单个配置

```bash
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py get \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"
```

### 修改配置

```bash
# 方式 1: 直接传入内容
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --content "配置内容" \
  --desc "修改说明"

# 方式 2: 从文件读取
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "my-config.yml" \
  --desc "修改说明"
```

### 同步到 SQL

```bash
# 同步单个配置
python3 .cursor/skills/manage-nacos-config/scripts/sync_to_sql.py \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"

# 同步所有配置
python3 .cursor/skills/manage-nacos-config/scripts/sync_to_sql.py --all

# 导出到新文件
python3 .cursor/skills/manage-nacos-config/scripts/sync_to_sql.py --export
```

## 4. 完整示例:修改 Redis 配置

```bash
# 步骤 1: 获取当前配置
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py get \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" > /tmp/app-config.yml

# 步骤 2: 编辑配置文件
# 使用你喜欢的编辑器修改 /tmp/app-config.yml

# 步骤 3: 更新到 Nacos
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py update \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP" \
  --file "/tmp/app-config.yml" \
  --desc "更新 Redis 配置"

# 步骤 4: 同步到 SQL (询问用户后)
python3 .cursor/skills/manage-nacos-config/scripts/sync_to_sql.py \
  --data-id "application-dev.yml" \
  --group "DEFAULT_GROUP"
```

## 5. 连接远程 Nacos

如果 Nacos 不在本地,可以通过参数指定:

```bash
python3 .cursor/skills/manage-nacos-config/scripts/nacos_config.py list \
  --host "nacos.example.com" \
  --port 8848 \
  --namespace "production"
```

## 6. 备份恢复

### 定期备份

```bash
# 导出当前所有配置
python3 .cursor/skills/manage-nacos-config/scripts/sync_to_sql.py --export

# 会生成类似 db/pig_config_export_20260129_143022.sql 的文件
```

### 从备份恢复

```bash
# 导入 SQL 到数据库
mysql -h127.0.0.1 -P33307 -uroot -proot pig_config < db/pig_config.sql

# 重启 Nacos 服务
```

## 7. 服务管理

### 查看所有服务

```bash
# 列出所有注册的服务
python3 .cursor/skills/manage-nacos-config/scripts/nacos_service.py \
  --host 127.0.0.1 --port 8848 list

# 显示实例统计
python3 .cursor/skills/manage-nacos-config/scripts/nacos_service.py \
  --host 127.0.0.1 --port 8848 list --with-instances
```

### 查看服务实例

```bash
# 查看服务的所有实例
python3 .cursor/skills/manage-nacos-config/scripts/nacos_service.py \
  --host 127.0.0.1 --port 8848 \
  get --service-name "pig-gateway"

# 只显示健康实例
python3 .cursor/skills/manage-nacos-config/scripts/nacos_service.py \
  --host 127.0.0.1 --port 8848 \
  instances --service-name "pig-gateway" --healthy-only
```

### 管理服务实例

```bash
# 标记实例为不健康(流量切走)
python3 .cursor/skills/manage-nacos-config/scripts/nacos_service.py \
  --host 127.0.0.1 --port 8848 \
  health --service-name "pig-gateway" \
  --ip "192.168.0.200" --port 9999 --healthy false

# 下线服务实例
python3 .cursor/skills/manage-nacos-config/scripts/nacos_service.py \
  --host 127.0.0.1 --port 8848 \
  deregister --service-name "pig-gateway" \
  --ip "192.168.0.200" --port 9999
```

## 常见问题

### Q: 提示缺少 requests 库?

```bash
pip install requests pyyaml
```

### Q: 连接 Nacos 失败?

检查:
1. Nacos 是否启动: `curl http://127.0.0.1:8848/nacos/`
2. 端口是否正确
3. 网络是否可达
4. 用户名密码是否正确(默认 nacos/nacos)

### Q: 同步到 SQL 失败?

确保:
1. `db/pig_config.sql` 文件存在
2. 配置在 SQL 文件中已存在(通过 data_id 和 group_id 匹配)
3. 有写入权限

### Q: 如何恢复误操作?

同步脚本会自动创建 `db/pig_config.sql.bak` 备份文件,可以从中恢复:

```bash
cp db/pig_config.sql.bak db/pig_config.sql
```

### Q: 服务下线后无法重新注册?

这是正常现象。服务实例需要重启才会重新注册到 Nacos。

## 更多信息

详细文档请查看:
- `SKILL.md` - 完整使用指南
- `examples.md` - 实际场景示例
