# Vault Module Test Suite

## 测试概述

本测试套件包含了Vault模块的完整集成测试，使用TestContainers来管理MySQL数据库。

## 测试类说明

### 1. VaultBalanceServiceTest
**位置**: `com.pig4cloud.pig.vault.service.VaultBalanceServiceTest`

**功能**: 测试VaultBalanceService的业务逻辑

**测试用例**:
- ✅ 充值成功
- ✅ 充值幂等性（相同refId）
- ✅ 无效资产符号的充值
- ✅ 不存在账户的充值
- ✅ 多次充值累加
- ✅ 获取当前用户余额
- ✅ 未认证用户获取余额
- ✅ 无账户用户获取余额
- ✅ 无效资产符号获取余额
- ✅ 充值后查询余额

### 2. VaultBalanceControllerTest
**位置**: `com.pig4cloud.pig.vault.controller.VaultBalanceControllerTest`

**功能**: 测试Vault余额相关的HTTP API接口

**测试用例**:
- ✅ 通过API成功充值
- ✅ 充值参数验证错误
- ✅ 充值幂等性
- ✅ 获取我的余额成功
- ✅ 无效资产符号获取余额
- ✅ 充值后查询余额
- ✅ 多次充值累加
- ✅ 不存在账户的充值

### 3. VaultControllerTest
**位置**: `com.pig4cloud.pig.vault.controller.VaultControllerTest`

**功能**: 测试Vault冻结相关的HTTP API接口

**测试用例**:
- ✅ 创建冻结
- ✅ 冻结幂等性
- ✅ 余额不足的冻结
- ✅ 参数验证错误
- ✅ 释放冻结
- ✅ 释放冻结幂等性
- ✅ 认领冻结
- ✅ 无效状态认领冻结
- ✅ 从HELD状态消费冻结
- ✅ 从CLAIMED状态消费冻结
- ✅ 获取余额
- ✅ 无效资产符号获取余额

## 运行测试

### 前置条件

1. **Docker**：TestContainers需要Docker来运行MySQL容器
   ```bash
   # 确保Docker正在运行
   docker --version
   ```

2. **Maven**：用于构建和运行测试
   ```bash
   mvn --version
   ```

### 运行所有测试

```bash
# 在项目根目录
cd pig-vault/pig-vault-biz
mvn clean test
```

### 运行特定测试类

```bash
# 运行Service层测试
mvn test -Dtest=VaultBalanceServiceTest

# 运行Controller层测试
mvn test -Dtest=VaultBalanceControllerTest

# 运行原有的Controller测试
mvn test -Dtest=VaultControllerTest
```

### 运行特定测试方法

```bash
# 运行单个测试方法
mvn test -Dtest=VaultBalanceServiceTest#testDepositSuccess
```

## TestContainers配置

### MySQL容器配置
- **镜像**: `mysql:8.0`
- **数据库名**: `pig-test`
- **用户名**: `test`
- **密码**: `test123456`
- **初始化脚本**: `db/vault_schema_test.sql`

### 容器复用

为了加快测试速度，TestContainers配置了容器复用功能。

在 `src/test/resources/testcontainers.properties` 中：
```properties
testcontainers.reuse.enable=true
```

这样，多次运行测试时会复用同一个MySQL容器，而不是每次都创建新容器。

## 测试数据初始化

每个测试方法执行前，会自动执行以下操作：

1. 清理测试数据
2. 插入测试账户（ID: 1001, USER_ID: 10001）
3. 插入测试资产（USDC）
4. 初始化余额（100 USDC可用，0冻结）

## 注意事项

1. **并发执行**: 测试类使用了`@TestMethodOrder`来保证测试顺序执行
2. **事务管理**: 部分测试使用了`@Transactional`注解来确保数据隔离
3. **安全上下文**: Service层测试需要Mock SecurityContext来模拟用户认证
4. **资源清理**: TestContainers会在测试结束后自动清理容器资源

## 常见问题

### 1. Docker未启动
**错误**: `Could not find a valid Docker environment`

**解决**: 启动Docker Desktop或Docker daemon

### 2. 端口冲突
**错误**: `Port 3306 is already in use`

**解决**: TestContainers会自动分配可用端口，如果仍有问题，检查是否有其他MySQL实例占用端口

### 3. 网络问题
**错误**: `Unable to download mysql:8.0 image`

**解决**: 
- 检查网络连接
- 配置Docker镜像加速器
- 或预先拉取镜像: `docker pull mysql:8.0`

## 测试覆盖率

运行测试覆盖率报告：

```bash
mvn clean test jacoco:report
```

报告位置: `target/site/jacoco/index.html`

## 相关文档

- [TestContainers文档](https://www.testcontainers.org/)
- [JUnit 5文档](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
