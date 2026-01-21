# 订单匹配错误处理

本模块使用 `pig-common-error` 记录和补偿交易事件处理失败的情况。

## 功能概述

当撮合引擎回调事件(Trade/Reduce/Reject)处理失败时,系统会自动将错误记录到 `error_record` 表,以便后续补偿重试。

## 错误记录的事件类型

### 1. Trade Event (交易事件)
- **Handler Key**: `order:handleTradeEvent`
- **Payload Class**: `FailedTradeEventDTO`
- **触发场景**: 当撮合引擎返回交易事件,但提交匹配失败时

### 2. Reduce Event (减量事件)
- **Handler Key**: `order:handleReduceEvent`
- **Payload Class**: `FailedReduceEventDTO`
- **触发场景**: 订单部分/全部取消时更新订单状态失败

### 3. Reject Event (拒绝事件)
- **Handler Key**: `order:handleRejectEvent`
- **Payload Class**: `FailedRejectEventDTO`
- **触发场景**: IOC 订单被拒绝时更新订单状态失败

## 补偿机制

### 自动补偿

可以配置定时任务自动批量补偿错误:

```java
@Component
public class OrderErrorCompensationScheduler {

    private final ErrorCompensationService compensationService;

    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void compensateOrderErrors() {
        // 批量补偿订单领域的错误,最多处理 100 条
        compensationService.compensateBatch("order", 100);
    }
}
```

### 手动补偿

#### 通过 API 单条补偿

```bash
POST /error/records/{id}/compensate
```

#### 通过 API 批量补偿

```bash
POST /error/records/compensate-batch?domain=order&limit=100
```

## 错误记录查询

### 分页查询错误记录

```bash
GET /error/records/page?page=1&size=10&domain=order&status=NEW
```

### 查询单条错误详情

```bash
GET /error/records/{id}
```

## 错误状态流转

1. **NEW** - 新记录,待补偿
2. **RETRYING** - 重试中
3. **RESOLVED** - 已解决
4. **DEAD** - 死信(超过最大重试次数,需要人工介入)

## 配置选项

在 `application.yml` 中可配置重试策略:

```yaml
pig:
  error:
    max-attempts: 5                      # 最大重试次数
    retry-delay-seconds: 60              # 重试延迟(秒)
    stack-trace-max-length: 4000         # 堆栈信息最大长度
    use-exponential-backoff: true        # 是否使用指数退避策略
    exponential-backoff-multiplier: 2.0  # 指数退避基数
```

## 实现细节

### 错误处理器

所有的错误补偿处理器都定义在 `TradeEventErrorHandler` 类中:

- `compensateFailedTradeEvent()` - 补偿失败的交易事件
- `compensateFailedReduceEvent()` - 补偿失败的减量事件
- `compensateFailedRejectEvent()` - 补偿失败的拒绝事件

### 错误记录

在 `MatcherEventHandler` 中,每个事件处理方法都包含错误捕获和记录逻辑:

```java
try {
    // 处理事件
    commitMatch(request);
} catch (Exception e) {
    // 记录错误
    errorRecordService.record("order", "order:handleTradeEvent", failedEvent, e);
}
```

## 监控建议

1. 定期检查 `error_record` 表中 `status=DEAD` 的记录
2. 监控错误记录的增长趋势,及时发现系统问题
3. 对于死信记录,需要人工分析原因并处理

## 相关文档

- [pig-common-error README](../../pig-common/pig-common-error/README.md)
- [错误补偿 API 文档](http://localhost:9999/doc.html)
