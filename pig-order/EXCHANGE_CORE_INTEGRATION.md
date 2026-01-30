# Exchange-Core Integration Summary

## Overview
This document summarizes the integration of the exchange-core matching engine into the pig-order module.

## Completed Tasks

### 1. Configuration Classes
- **ExchangeCoreConfiguration.java**: Spring configuration for ExchangeCore
  - Creates ExchangeCore bean with NO_RISK_PROCESSING mode (Direct risk mode)
  - Registers custom IEventsHandler for matching callbacks
  - Implements SmartLifecycle for proper startup/shutdown management

- **MatchingEngineProperties.java**: Configuration properties
  - `default-asset`: Default asset symbol ID (default: 1 for USDC)
  - `enable-state-recovery`: Enable state recovery on startup (default: true)

### 2. Domain Converters
- **OrderCommandConverter.java**: Converts between domain Order and ExchangeCore API commands
  - `toApiPlaceOrder()`: Converts Order to ApiPlaceOrder
    - Reserve price: BUY orders +20%, SELL orders -20%
    - Price/size scaling: multiply by 100 for 2 decimal places
  - `toApiCancelOrder()`: Converts to ApiCancelOrder

### 3. Event Handlers
- **OrderEventsHandler.java**: Implements IEventsHandler interface
- **OrderMatchService.java**: Service interface for processing matching events
- **OrderMatchServiceImpl.java**: Implementation
  - `handleTradeEvent()`: Processes trade events, calls existing `commitMatch()` method
    - matchId format: `{takerOrderId}-{timestamp}`
    - idempotencyKey uses matchId
  - `handleReduceEvent()`: Handles order cancellation/reduction
  - `handleRejectEvent()`: Handles IOC order rejection
  - All failures include TODO comments for retry mechanisms

### 4. State Recovery
- **OrderStateRecoveryService.java**: Recovers matching engine state on startup
  - Listens to ApplicationReadyEvent
  - Recovery order: MATCHING status first, then OPEN and PARTIALLY_FILLED
  - Uses remaining quantity (not original quantity) for recovery
  - Can be disabled via configuration

### 5. OrderServiceImpl Integration
- **createOrder()**:
  - Creates order in CREATED status
  - Submits to matching engine via `exchangeApi.submitCommandAsync()`
  - Waits for result synchronously (`.join()`)
  - Updates to MATCHING status on success
  - Throws exception on failure (transaction rollback)

- **cancelOrder()**:
  - Market orders: rejected with `MARKET_ORDER_CANCEL_NOT_SUPPORTED` (no ApiCancelOrder)
  - Limit orders: updates to CANCEL_REQUESTED, submits cancel to matching engine
  - Waits for result synchronously
  - Throws exception on failure (transaction rollback)

### 6. Configuration
- **application.yml**: Added matching-engine configuration section
  ```yaml
  matching-engine:
    default-asset: 1
    enable-state-recovery: true
  ```

## Order State Machine: Market Orders (市价单)

- **Initial**: OPEN → 提交撮合 → MATCHING
- **撮合回调**:
  - 成交量 = 0 → REJECTED（终态）
  - 成交量 = 全部 → FILLED（终态）
  - 成交量 ∈ (0, 全部) → PARTIALLY_FILLED（终态，部分成交后剩余不再撮合）
- **终态**：市价单不允许进入 CANCEL_REQUESTED / CANCELLED；取消接口对市价单直接拒绝（错误码 `MARKET_ORDER_CANCEL_NOT_SUPPORTED`）。

**关键语义**：PARTIALLY_FILLED 对市价单为终态，表示“部分成交后剩余取消，不再继续撮合”；市价单取消请求无效。

## Key Design Decisions

### 1. Synchronous Submission
Orders are submitted to the matching engine synchronously within the transaction:
- Success: Order moves to MATCHING status
- Failure: Transaction rolls back, order creation/cancellation fails

### 2. No Version Conflict
Avoided version conflict by:
- Creating order in CREATED status
- Submitting to matching engine
- Single update to MATCHING status (only one `updateById` call)

### 3. Idempotency
- Trade events use matchId as idempotencyKey
- Existing `commitMatch()` method handles idempotency via matchId

### 4. Error Handling
- Matching engine failures cause transaction rollback
- Event processing failures logged with TODO comments for retry mechanisms
- State recovery failures logged but don't stop application startup

## Files Created/Modified

### Created Files
1. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/ExchangeCoreConfiguration.java`
2. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/MatchingEngineProperties.java`
3. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/OrderCommandConverter.java`
4. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/OrderEventsHandler.java`
5. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/OrderMatchService.java`
6. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/OrderMatchServiceImpl.java`
7. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/match/OrderStateRecoveryService.java`

### Modified Files
1. `pig-order-biz/src/main/java/com/pig4cloud/pig/order/service/impl/OrderServiceImpl.java`
   - Added exchangeApi dependency
   - Integrated matching engine submission in createOrder()
   - Integrated matching engine cancel in cancelOrder()
2. `pig-order-biz/src/main/resources/application.yml`
   - Added matching-engine configuration
3. `pig-order-biz/pom.xml`
   - Removed empty dependency declaration

## Testing Recommendations

### Unit Tests
- Test OrderCommandConverter price/size scaling
- Test OrderCommandConverter reserve price calculation
- Mock ExchangeApi for OrderServiceImpl tests

### Integration Tests
- Test full order lifecycle with matching engine
- Test state recovery with various order states
- Test transaction rollback on matching engine failure
- Test idempotency of trade event processing

### Load Tests
- Test concurrent order submissions
- Test matching engine throughput
- Monitor memory usage with state recovery

## Future Improvements (TODOs in Code)

1. **Retry Mechanisms**
   - Implement retry for failed trade event processing
   - Implement retry for failed reduce event processing
   - Implement retry for failed reject event processing
   - Implement retry for failed state recovery

2. **Configuration**
   - Make price/size scaling factor configurable per market
   - Make reserve price percentage configurable

3. **Monitoring**
   - Add metrics for matching engine performance
   - Add alerts for event processing failures
   - Add dashboard for state recovery status

4. **Fee Calculation**
   - Implement fee calculation in handleTradeEvent()

## Notes

- Exchange-core uses Direct risk mode (NO_RISK_PROCESSING) as balance tracking is handled by pig-vault
- Journal/snapshot flush is disabled, order table is the single source of truth
- Order table serves as the authority for matching engine state recovery
- MarketId is used as symbolId for exchange-core
