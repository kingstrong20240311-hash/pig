# pig-common-error

错误记录与补偿模块，提供跨服务通用的"错误记录 + 统一补偿执行"能力。

## 功能特性

- **错误记录**：自动保存出错数据、处理函数、处理方式和执行状态
- **统一补偿**：基于记录重新执行处理函数
- **补偿接口**：提供单条补偿和批量补偿能力
- **灵活重试策略**：支持固定延迟和指数退避两种重试策略
- **状态管理**：NEW -> RETRYING -> RESOLVED/DEAD 完整状态流转

## 快速开始

### 1. 添加依赖

在需要使用错误记录功能的模块中添加依赖：

```xml
<dependency>
    <groupId>com.pig4cloud</groupId>
    <artifactId>pig-common-error</artifactId>
</dependency>
```

### 2. 执行数据库脚本

执行 `db/error_record.sql` 创建 `error_record` 表。

### 3. 配置（可选）

在 `application.yml` 中添加配置（以下为默认值）：

```yaml
pig:
  error:
    max-attempts: 5                      # 最大重试次数
    retry-delay-seconds: 60              # 重试延迟（秒）
    stack-trace-max-length: 4000         # 堆栈信息最大长度
    use-exponential-backoff: true        # 是否使用指数退避策略
    exponential-backoff-multiplier: 2.0  # 指数退避基数
```

## 使用示例

### 1. 定义错误处理器

使用 `@ErrorHandler` 注解标注处理方法：

```java
@Service
public class OrderErrorHandler {

    @ErrorHandler(domain = "order", key = "onCancel", payloadClass = OrderCancelEvent.class)
    public void handleOrderCancel(OrderCancelEvent event) {
        // 处理订单取消补偿逻辑
        orderService.cancelOrder(event.getOrderId());
    }
}
```

### 2. 记录错误

在业务代码中捕获异常并记录：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ErrorRecordService errorRecordService;

    public void processOrder(OrderCancelEvent event) {
        try {
            // 业务逻辑
            doSomething(event);
        } catch (Exception e) {
            // 记录错误，等待后续补偿
            errorRecordService.record(
                "order",                              // 领域
                "order:onCancel",                     // 处理器key
                event,                                // 原始数据对象
                e                                     // 异常
            );
        }
    }
}
```

### 3. 执行补偿

#### 单条补偿（通过 API）

```bash
POST /error/records/{id}/compensate
```

#### 批量补偿（通过 API）

```bash
POST /error/records/compensate-batch?domain=order&limit=100
```

#### 程序化调用

```java
@Service
@RequiredArgsConstructor
public class CompensationScheduler {

    private final ErrorCompensationService compensationService;

    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void compensateErrors() {
        // 批量补偿订单领域的错误
        compensationService.compensateBatch("order", 100);
    }
}
```

## API 接口

### 单条补偿

```
POST /error/records/{id}/compensate
```

### 批量补偿

```
POST /error/records/compensate-batch?domain={domain}&limit={limit}
```

- `domain`：领域（可选，不传则补偿所有领域）
- `limit`：限制数量（默认100）

### 分页查询

```
GET /error/records/page?page=1&size=10&domain={domain}&status={status}
```

### 查询详情

```
GET /error/records/{id}
```

## 核心概念

### 错误记录状态

- **NEW**：新记录，待补偿
- **RETRYING**：重试中
- **RESOLVED**：已解决
- **DEAD**：死信（超过最大重试次数）

### Handler Key

Handler Key 由 `domain` 和 `key` 组成，格式为 `domain:key`。

例如：`order:onCancel`

### 重试策略

- **固定延迟**：每次重试间隔相同
- **指数退避**：重试间隔呈指数增长（delay * multiplier^(attempts-1)），最大1小时

## 注意事项

1. 处理器方法支持以下参数类型：
   - 强类型对象（通过 `payloadClass` 指定）
   - JSON 字符串（不指定 `payloadClass`）
   - 无参数

2. 建议在定时任务中周期性调用批量补偿

3. 死信记录需要人工介入处理

4. 错误记录中的时间字段使用 `Instant` 类型，与项目中现有的 `LocalDateTime` 区分

## 设计原则

- **独立部署**：作为 common 模块，可被任意服务依赖
- **不侵入业务**：通过注解方式注册处理器，业务代码无感知
- **灵活扩展**：支持自定义重试策略和补偿逻辑
