# Changelog

## [Unreleased] - 2026-01-22

### 新增
- ✨ 初始化 `pig-e2e-test` 模块
- ✨ 添加基础测试类 `E2eBaseTest`，提供：
  - 环境变量读取与验证
  - HTTP 客户端配置（REST Assured）
  - 通用鉴权方法（登录、Token 管理）
  - 公共断言方法
  - 轮询查询工具（Awaitility）
- ✨ 添加订单流程测试 `OrderFlowE2eTest`：
  - 完整下单流程测试
  - 市价单立即成交测试
  - 订单部分成交测试
- ✨ 添加撤单流程测试 `CancelFlowE2eTest`：
  - 单个订单撤单测试
  - 部分成交后撤单测试
  - 批量撤单测试
  - 重复撤单幂等性测试
  - 已成交订单无法撤单测试
- 📝 添加完整文档：
  - `README.md` - 详细使用文档
  - `QUICKSTART.md` - 快速开始指南
  - `env.template` - 环境变量配置模板
- 🔧 添加执行脚本 `run-e2e-test.sh`
- 🔧 配置 Maven Failsafe Plugin 用于集成测试
- 🔧 配置日志输出（Logback）

### 依赖
- JUnit 5 - 测试框架
- REST Assured 5.5.0 - HTTP 客户端
- Awaitility 4.2.2 - 异步断言
- Jackson - JSON 序列化
- Lombok - 代码简化
- Logback - 日志

### 环境变量
- `PIG_GATEWAY_URL` - Gateway 地址（必填）
- `PIG_TEST_USERNAME` - 测试用户名（可选）
- `PIG_TEST_PASSWORD` - 测试密码（可选）
- `PIG_ADMIN_TOKEN` - 管理员 Token（可选）
- `PIG_CONNECTION_TIMEOUT` - 连接超时（可选）
- `PIG_READ_TIMEOUT` - 读取超时（可选）
- `PIG_MAX_WAIT_SECONDS` - 轮询最大等待时间（可选）
- `PIG_POLL_INTERVAL_MILLIS` - 轮询间隔（可选）

### 执行方式
```bash
# Maven 命令行
PIG_GATEWAY_URL=http://127.0.0.1:9999 mvn -pl pig-e2e-test -Pe2e verify

# 使用脚本
./pig-e2e-test/run-e2e-test.sh
```

## 未来计划

### 待添加功能
- [ ] 支付流程 E2E 测试
- [ ] 提现流程 E2E 测试
- [ ] 用户注册/登录流程测试
- [ ] 并发订单测试
- [ ] 压力测试场景
- [ ] 数据清理工具
- [ ] 测试数据生成器
- [ ] 测试报告美化（HTML/Allure）

### 优化计划
- [ ] 支持多环境配置文件切换
- [ ] 添加测试数据隔离机制
- [ ] 支持并行执行测试
- [ ] 添加性能指标收集
- [ ] 集成 CI/CD 流水线
- [ ] 添加失败重试机制
- [ ] 支持分布式测试执行

## 注意事项

1. **环境准备**：确保所有服务已启动
2. **数据隔离**：建议使用独立测试数据库
3. **并发限制**：当前不支持并行执行
4. **超时配置**：根据实际环境调整超时时间
