# pig-e2e-test 项目总览

## 📚 项目概述

`pig-e2e-test` 是 Pig 项目的端到端（E2E）测试模块，专注于 HTTP 级别的全链路测试。该模块假设所有服务已经启动，通过 HTTP API 进行业务流程验证。

**设计理念**：
- ✅ 只测试 HTTP 接口，不依赖内部实现
- ✅ 外部服务已启动，通过环境变量配置服务地址
- ✅ 关注业务流程，验证端到端的正确性
- ✅ 轻量级，易于集成到 CI/CD

## 📁 目录结构

```
pig-e2e-test/
├── .gitignore                              # Git 忽略配置
├── pom.xml                                 # Maven 配置
├── README.md                               # 详细使用文档
├── QUICKSTART.md                           # 快速开始指南
├── CHANGELOG.md                            # 变更日志
├── PROJECT_OVERVIEW.md                     # 本文档
├── env.template                            # 环境变量配置模板
├── run-e2e-test.sh                         # 测试执行脚本
└── src/test/
    ├── java/com/pig4cloud/pig/e2e/
    │   ├── E2eBaseTest.java               # 测试基类
    │   │   ├── 环境变量读取与验证
    │   │   ├── HTTP 客户端配置（REST Assured）
    │   │   ├── 通用鉴权方法（登录、Token 管理）
    │   │   ├── 公共断言方法
    │   │   └── 轮询查询工具（Awaitility）
    │   │
    │   ├── OrderFlowE2eTest.java          # 订单流程测试
    │   │   ├── testCompleteOrderFlow()    # 完整下单流程
    │   │   ├── testMarketOrderImmediateExecution() # 市价单
    │   │   └── testPartialOrderFill()     # 部分成交
    │   │
    │   └── CancelFlowE2eTest.java         # 撤单流程测试
    │       ├── testSingleOrderCancellation()  # 单个撤单
    │       ├── testCancelPartiallyFilledOrder() # 部分成交后撤单
    │       ├── testBatchOrderCancellation()   # 批量撤单
    │       ├── testDuplicateCancellation()    # 重复撤单（幂等性）
    │       └── testCannotCancelFilledOrder()  # 已成交订单无法撤单
    │
    └── resources/
        ├── application-e2e.yml             # 测试配置
        └── logback-test.xml                # 日志配置
```

## 🎯 核心功能

### 1. 测试基类（E2eBaseTest）

**职责**：
- 读取并验证环境变量
- 配置 HTTP 客户端（REST Assured）
- 提供统一的鉴权机制
- 提供公共断言方法
- 提供轮询查询工具

**关键方法**：
```java
// 环境变量读取
protected static String gatewayUrl;
protected static String testUsername;
protected static String testPassword;

// 鉴权
protected String login(String username, String password)
protected RequestSpecification authenticatedRequest()

// 断言
protected void assertSuccess(Response response)
protected void assertFieldEquals(Response response, String fieldPath, Object expectedValue)

// 轮询查询
protected Response pollUntil(Supplier<Response> pollAction, 
                             Predicate<Response> condition, 
                             String message)
```

### 2. 订单流程测试（OrderFlowE2eTest）

**测试场景**：
1. **完整下单流程**：创建买单 → 创建卖单 → 撮合 → 验证成交
2. **市价单测试**：市价单立即成交
3. **部分成交测试**：大额订单部分成交

**验证点**：
- ✅ 订单状态变化（NEW → PARTIAL_FILLED → FILLED）
- ✅ 成交记录生成
- ✅ 成交价格、数量正确
- ✅ 余额变化正确

### 3. 撤单流程测试（CancelFlowE2eTest）

**测试场景**：
1. **单个撤单**：创建订单 → 撤单 → 验证状态
2. **部分成交后撤单**：部分成交 → 撤单剩余部分
3. **批量撤单**：创建多个订单 → 批量撤单
4. **幂等性测试**：重复撤单应不报错
5. **边界测试**：已成交订单无法撤单

**验证点**：
- ✅ 订单状态变化（NEW → CANCELLED）
- ✅ 已成交数量不变
- ✅ 冻结资金释放
- ✅ 幂等性保证

## 🔧 技术栈

| 技术 | 版本 | 用途 |
|-----|------|------|
| JUnit 5 | - | 测试框架 |
| REST Assured | 5.5.0 | HTTP 客户端 |
| Awaitility | 4.2.2 | 异步断言/轮询 |
| Jackson | - | JSON 序列化 |
| Lombok | - | 代码简化 |
| Logback | - | 日志输出 |
| Maven Failsafe | 3.5.2 | 集成测试插件 |

## 🚀 使用方式

### 快速开始

```bash
# 1. 配置环境变量
export PIG_GATEWAY_URL=http://127.0.0.1:9999

# 2. 执行测试
mvn -pl pig-e2e-test -Pe2e verify

# 或使用脚本
./pig-e2e-test/run-e2e-test.sh
```

### 高级用法

```bash
# 只运行特定测试
mvn -pl pig-e2e-test -Pe2e verify -Dit.test=OrderFlowE2eTest

# 使用自定义配置
PIG_GATEWAY_URL=http://test.example.com \
PIG_MAX_WAIT_SECONDS=60 \
mvn -pl pig-e2e-test -Pe2e verify

# 使用 Admin Token 跳过登录
PIG_ADMIN_TOKEN=your_token \
mvn -pl pig-e2e-test -Pe2e verify
```

## 📊 测试流程

```
┌─────────────────┐
│  环境准备        │
│  - 启动服务      │
│  - 初始化数据    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  读取环境变量    │
│  - Gateway URL  │
│  - 测试账号      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  登录获取 Token │
│  (或使用 Admin  │
│   Token)        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  执行测试用例    │
│  - 发送 HTTP    │
│  - 轮询查询      │
│  - 验证结果      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  生成测试报告    │
│  - JUnit 报告   │
│  - 日志文件      │
└─────────────────┘
```

## 🔍 测试验证点

### 订单流程
- [x] 订单创建成功，返回订单 ID
- [x] 订单状态正确变化（NEW → PARTIAL_FILLED → FILLED）
- [x] 撮合成功，生成成交记录
- [x] 成交价格、数量符合预期
- [x] 余额变化正确

### 撤单流程
- [x] 撤单成功，订单状态变为 CANCELLED
- [x] 部分成交后撤单，已成交数量不变
- [x] 批量撤单成功
- [x] 重复撤单幂等（不报错）
- [x] 已成交订单无法撤单（返回错误）
- [x] 冻结资金正确释放

## 📈 未来规划

### 短期计划
- [ ] 添加支付流程测试
- [ ] 添加提现流程测试
- [ ] 添加用户注册/登录测试
- [ ] 优化测试数据隔离

### 中期计划
- [ ] 支持多环境配置切换
- [ ] 支持并行执行测试
- [ ] 集成 Allure 测试报告
- [ ] 添加性能指标收集

### 长期计划
- [ ] 支持分布式测试执行
- [ ] 添加压力测试场景
- [ ] 集成 CI/CD 流水线
- [ ] 自动化测试数据生成

## 🤝 贡献指南

### 添加新测试用例

1. **继承 E2eBaseTest**：
```java
@Slf4j
@DisplayName("新功能 E2E 测试")
public class NewFeatureE2eTest extends E2eBaseTest {
    // 测试方法
}
```

2. **使用基类提供的方法**：
```java
@Test
public void testNewFeature() {
    // 使用 authenticatedRequest() 发送请求
    Response response = authenticatedRequest()
        .body(request)
        .post("/api/new-feature");
    
    // 使用断言方法
    assertSuccess(response);
    assertFieldEquals(response, "status", "SUCCESS");
    
    // 使用轮询工具
    pollUntil(() -> queryStatus(id), 
              r -> "COMPLETED".equals(r.jsonPath().getString("status")),
              "操作未完成");
}
```

3. **编写清晰的测试文档**

### 代码规范
- 使用 `@DisplayName` 注解说明测试用例
- 每个测试方法关注单一场景
- 使用 `log` 记录关键步骤
- 编写详细的断言消息

## 📞 技术支持

- **文档**：[README.md](README.md)
- **快速开始**：[QUICKSTART.md](QUICKSTART.md)
- **环境配置**：[env.template](env.template)
- **变更日志**：[CHANGELOG.md](CHANGELOG.md)

## 📄 许可证

Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0.
