# 🚀 快速开始

## 前置条件

1. ✅ JDK 21+ 已安装
2. ✅ Maven 3.6+ 已安装
3. ✅ 所有服务已启动（Gateway、Order、Vault 等）
4. ✅ 数据库数据已初始化
5. ✅ 测试账号已创建

## 3 分钟快速上手

### 步骤 1：配置环境变量

```bash
# 方式 A：直接设置环境变量
export PIG_GATEWAY_URL=http://127.0.0.1:9999

# 方式 B：使用 .env 文件（推荐）
cp .env.example .env
# 编辑 .env 文件，设置实际的 Gateway 地址
```

### 步骤 2：执行测试

```bash
# 在项目根目录执行
cd /path/to/pig/back

# 运行所有 E2E 测试
PIG_GATEWAY_URL=http://127.0.0.1:9999 mvn -pl pig-e2e-test -Pe2e verify

# 或使用脚本（更简单）
./pig-e2e-test/run-e2e-test.sh
```

### 步骤 3：查看结果

```bash
# 查看测试报告
ls pig-e2e-test/target/failsafe-reports/

# 查看日志
cat pig-e2e-test/target/logs/e2e-test.log
```

## 常用命令

### 只运行特定测试

```bash
# 只运行订单流程测试
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=OrderFlowE2eTest

# 只运行撤单流程测试
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=CancelFlowE2eTest

# 运行特定测试方法
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=OrderFlowE2eTest#testCompleteOrderFlow
```

### 使用自定义配置

```bash
# 使用自定义超时时间
PIG_GATEWAY_URL=http://127.0.0.1:9999 \
PIG_MAX_WAIT_SECONDS=60 \
mvn -pl pig-e2e-test -Pe2e verify

# 使用 Admin Token 跳过登录
PIG_GATEWAY_URL=http://127.0.0.1:9999 \
PIG_ADMIN_TOKEN=your_token_here \
mvn -pl pig-e2e-test -Pe2e verify
```

### 调试模式

```bash
# 启用 Debug 日志
mvn -pl pig-e2e-test -Pe2e verify -X

# 跳过测试失败继续执行
mvn -pl pig-e2e-test -Pe2e verify -Dmaven.test.failure.ignore=true
```

## 在 IDE 中运行

### IntelliJ IDEA

1. 打开测试类（如 `OrderFlowE2eTest.java`）
2. 点击 Run/Debug Configurations
3. 添加环境变量：
   ```
   PIG_GATEWAY_URL=http://127.0.0.1:9999
   ```
4. 右键点击测试方法 → Run

### VS Code

1. 安装 Java Test Runner 扩展
2. 在 `.vscode/settings.json` 添加：
   ```json
   {
     "java.test.config": {
       "env": {
         "PIG_GATEWAY_URL": "http://127.0.0.1:9999"
       }
     }
   }
   ```
3. 点击测试方法旁的 Run 按钮

## 故障排查

### 问题 1：Gateway 连接失败

```
错误：环境变量 PIG_GATEWAY_URL 未配置
解决：export PIG_GATEWAY_URL=http://127.0.0.1:9999
```

### 问题 2：登录失败

```
错误：登录失败：未获取到 access_token
解决：
1. 检查测试账号是否存在
2. 检查用户名密码是否正确
3. 或使用 PIG_ADMIN_TOKEN 跳过登录
```

### 问题 3：订单撮合超时

```
错误：买单撮合超时，未达到 PARTIAL_FILLED 或 FILLED 状态
解决：
1. 检查撮合引擎是否启动
2. 增加超时时间：PIG_MAX_WAIT_SECONDS=60
3. 检查订单价格是否匹配
```

### 问题 4：Maven 依赖下载失败

```
错误：Could not resolve dependencies
解决：
1. 检查网络连接
2. 配置 Maven 镜像（如阿里云镜像）
3. 清理本地仓库：mvn clean
```

## 下一步

- 📖 阅读 [README.md](README.md) 了解完整文档
- 🔍 查看 [E2eBaseTest.java](src/test/java/com/pig4cloud/pig/e2e/E2eBaseTest.java) 了解基础测试类
- ✍️ 编写自己的 E2E 测试用例

## 技术支持

- 文档：[README.md](README.md)
- 问题反馈：项目 Issues
- 代码示例：[OrderFlowE2eTest.java](src/test/java/com/pig4cloud/pig/e2e/OrderFlowE2eTest.java)
