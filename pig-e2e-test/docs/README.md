# pig-e2e-test - 端到端测试模块

## 📋 概述

`pig-e2e-test` 是 Pig 项目的 HTTP 级别端到端测试模块，用于验证完整的业务流程。测试假设所有服务已启动，通过 HTTP API 进行全链路验证。

## 🏗️ 模块结构

```
pig-e2e-test/
├── pom.xml                                 # Maven 配置
├── README.md                               # 本文档
└── src/test/
    ├── java/com/pig4cloud/pig/e2e/
    │   ├── E2eBaseTest.java               # 测试基类（环境变量、HTTP 客户端、公共断言）
    │   ├── OrderFlowE2eTest.java          # 订单流程测试
    │   └── CancelFlowE2eTest.java         # 撤单流程测试
    └── resources/
        ├── application-e2e.yml             # 测试配置
        └── logback-test.xml                # 日志配置
```

## 🔧 环境变量配置

### 必填项

| 环境变量 | 说明 | 示例 |
|---------|------|------|
| `PIG_GATEWAY_URL` | Gateway 服务地址 | `http://127.0.0.1:9999` |

### 可选项（有默认值）

| 环境变量 | 说明 | 默认值 |
|---------|------|-------|
| `PIG_TEST_USERNAME` | 测试用户名 | `test_user` |
| `PIG_TEST_PASSWORD` | 测试密码 | `test_password` |
| `PIG_ADMIN_TOKEN` | 管理员 Token（跳过登录） | 无 |
| `PIG_CONNECTION_TIMEOUT` | 连接超时（毫秒） | `30000` |
| `PIG_READ_TIMEOUT` | 读取超时（毫秒） | `60000` |
| `PIG_MAX_WAIT_SECONDS` | 轮询最大等待时间（秒） | `30` |
| `PIG_POLL_INTERVAL_MILLIS` | 轮询间隔（毫秒） | `500` |

## 🚀 执行测试

### 方式 1：使用 Maven 命令行

```bash
# 设置必填环境变量并执行测试
PIG_GATEWAY_URL=http://127.0.0.1:9999 mvn -pl pig-e2e-test -Pe2e verify

# 使用自定义配置
PIG_GATEWAY_URL=http://192.168.1.100:9999 \
PIG_TEST_USERNAME=admin \
PIG_TEST_PASSWORD=admin123 \
PIG_MAX_WAIT_SECONDS=60 \
mvn -pl pig-e2e-test -Pe2e verify

# 使用 Admin Token 跳过登录
PIG_GATEWAY_URL=http://127.0.0.1:9999 \
PIG_ADMIN_TOKEN=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9... \
mvn -pl pig-e2e-test -Pe2e verify
```

### 方式 2：使用 Shell 脚本

创建 `run-e2e-test.sh`：

```bash
#!/bin/bash

# 设置环境变量
export PIG_GATEWAY_URL=http://127.0.0.1:9999
export PIG_TEST_USERNAME=test_user
export PIG_TEST_PASSWORD=test_password

# 执行测试
mvn -pl pig-e2e-test -Pe2e verify
```

执行：

```bash
chmod +x run-e2e-test.sh
./run-e2e-test.sh
```

### 方式 3：使用 IDE（IDEA / Eclipse）

1. **配置环境变量**：
   - 打开 Run/Debug Configurations
   - 添加 Environment Variables：
     ```
     PIG_GATEWAY_URL=http://127.0.0.1:9999
     ```

2. **运行测试类**：
   - 右键点击测试类（如 `OrderFlowE2eTest.java`）
   - 选择 "Run" 或 "Debug"

## 📝 测试用例说明

### OrderFlowE2eTest - 订单流程测试

| 测试用例 | 说明 |
|---------|------|
| `testCompleteOrderFlow` | 完整下单流程：创建订单 → 撮合 → 成交验证 |
| `testMarketOrderImmediateExecution` | 市价单立即成交 |
| `testPartialOrderFill` | 订单部分成交 |

**测试流程**：
1. 创建买单/卖单
2. 等待撮合完成（轮询查询）
3. 验证订单状态（NEW → PARTIAL_FILLED → FILLED）
4. 查询成交记录
5. 验证成交详情（价格、数量）
6. 验证余额变化

### CancelFlowE2eTest - 撤单流程测试

| 测试用例 | 说明 |
|---------|------|
| `testSingleOrderCancellation` | 单个订单撤单流程 |
| `testCancelPartiallyFilledOrder` | 部分成交后撤单 |
| `testBatchOrderCancellation` | 批量撤单 |
| `testDuplicateCancellation` | 重复撤单（幂等性） |
| `testCannotCancelFilledOrder` | 已成交订单无法撤单 |

**测试流程**：
1. 创建订单
2. 执行撤单操作
3. 轮询查询订单状态（NEW → CANCELLED）
4. 验证冻结资金释放
5. 验证订单记录完整性

## 🛠️ 技术栈

- **测试框架**：JUnit 5
- **HTTP 客户端**：REST Assured
- **异步断言**：Awaitility
- **JSON 处理**：Jackson
- **日志**：SLF4J + Logback
- **构建工具**：Maven Failsafe Plugin

## 📊 测试报告

测试完成后，查看报告：

```bash
# 测试报告位置
target/failsafe-reports/

# 查看 HTML 报告（需要 maven-surefire-report-plugin）
mvn surefire-report:failsafe-report-only
open target/site/failsafe-report.html
```

## 🐛 调试技巧

### 1. 查看详细日志

修改 `logback-test.xml`：

```xml
<logger name="io.restassured" level="DEBUG"/>
<logger name="com.pig4cloud" level="DEBUG"/>
```

### 2. 增加轮询等待时间

```bash
PIG_MAX_WAIT_SECONDS=120 mvn -pl pig-e2e-test -Pe2e verify
```

### 3. 单独运行特定测试

```bash
# 只运行订单流程测试
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=OrderFlowE2eTest

# 只运行特定测试方法
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=OrderFlowE2eTest#testCompleteOrderFlow
```

### 4. 跳过测试失败继续执行

```bash
mvn -pl pig-e2e-test -Pe2e verify -Dmaven.test.failure.ignore=true
```

## 📌 注意事项

1. **环境准备**：
   - 确保所有服务（Gateway、Order、Vault 等）已启动
   - 确保数据库数据初始化完成
   - 确保测试账号已创建且有权限

2. **数据隔离**：
   - 每次测试使用唯一的订单 ID
   - 建议使用独立的测试数据库
   - 测试后可选择清理数据

3. **并发执行**：
   - 默认不支持并行执行（避免数据冲突）
   - 如需并行，需确保测试数据完全隔离

4. **超时设置**：
   - 根据实际环境调整超时时间
   - 撮合引擎性能不同，等待时间可能不同

## 🔗 相关模块

- **pig-order**：订单服务
- **pig-vault**：资金服务
- **pig-gateway**：网关服务

## 📄 许可证

Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0.
