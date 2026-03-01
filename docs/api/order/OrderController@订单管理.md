# 订单管理（OrderController）

- Controller：`OrderController`
- Tag：`订单管理`
- Base Path：（无，方法路径即完整路径）
- 鉴权：需要 Bearer Token

## 1. 创建订单

- 方法/路径：`POST /create`
- Summary：创建订单
- Description：创建新订单并进行冻结。`userId` 由后端从认证上下文自动填充，客户端无需传递。
- 权限：`@pms.hasPermission('order_create')`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| marketId | long | 是 | 市场ID |
| outcome | string | 是 | 结果：`YES` / `NO` |
| side | string | 是 | 方向：`BUY` / `SELL` |
| type | string | 是 | 订单类型：`LIMIT` / `MARKET` |
| price | number | 否 | 价格（LIMIT 订单必填） |
| quantity | number | 是 | 数量（必须大于0） |
| timeInForce | string | 否 | 有效期：`GTC` / `IOC` / `FOK` / `GTD`（默认 GTC） |
| expireAt | timestamp（epoch ms） | 否 | 过期时间（GTD 订单使用） |
| idempotencyKey | string | 是 | 幂等键 |

请求示例：

```json
{
  "marketId": 1001,
  "outcome": "YES",
  "side": "BUY",
  "type": "LIMIT",
  "price": "0.65",
  "quantity": "100",
  "timeInForce": "GTC",
  "idempotencyKey": "order-abc-20260101"
}
```

### 响应 data（`CreateOrderResponse`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| orderId | string | 订单ID |
| status | string | 订单状态：`OPEN` / `MATCHING` / `PARTIALLY_FILLED` / `FILLED` / `REJECTED` |
| remainingQuantity | number | 剩余可成交数量 |
| rejectReason | string | 拒绝原因（status 为 REJECTED 时有值） |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderId": "9000000001",
    "status": "OPEN",
    "remainingQuantity": "100",
    "rejectReason": null
  }
}
```

---

## 2. 取消订单

- 方法/路径：`POST /cancel`
- Summary：取消订单
- Description：请求取消订单；市价单不支持取消（返回错误 `MARKET_ORDER_CANCEL_NOT_SUPPORTED`）
- 权限：`@pms.hasPermission('order_cancel')`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| orderId | long | 是 | 订单ID |
| reason | string | 否 | 取消原因 |
| idempotencyKey | string | 是 | 幂等键 |

请求示例：

```json
{
  "orderId": 9000000001,
  "reason": "用户主动取消",
  "idempotencyKey": "cancel-abc-20260101"
}
```

### 响应 data（`CancelOrderResponse`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| orderId | string | 订单ID |
| status | string | 取消后状态：`CANCEL_REQUESTED` / `CANCELLED` |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderId": "9000000001",
    "status": "CANCEL_REQUESTED"
  }
}
```

---

## 3. 提交撮合结果

- 方法/路径：`POST /commit-match`
- Summary：提交撮合结果
- Description：撮合引擎提交成交明细，一个 Taker 可匹配多个 Maker
- 权限：`@pms.hasPermission('order_match_commit')`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| matchId | string | 是 | 撮合ID（幂等主键） |
| takerOrderId | long | 是 | Taker 订单ID |
| fills | array | 是 | Maker 成交明细列表（至少 1 条） |
| idempotencyKey | string | 是 | 幂等键 |

`fills[]` 字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| makerOrderId | long | 是 | Maker 订单ID |
| price | number | 是 | 成交价格（必须大于0） |
| quantity | number | 是 | 成交数量（必须大于0） |
| fee | number | 否 | 手续费 |

请求示例：

```json
{
  "matchId": "match-20260101-001",
  "takerOrderId": 9000000002,
  "fills": [
    {
      "makerOrderId": 9000000001,
      "price": "0.65",
      "quantity": "50",
      "fee": "0.001"
    }
  ],
  "idempotencyKey": "commit-match-20260101-001"
}
```

### 响应 data（`CommitMatchResponse`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| matchId | string | 撮合ID |
| orderStates | object | 订单状态映射（orderId → OrderStateDTO） |
| settlementRequired | boolean | 是否需要结算 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "matchId": "match-20260101-001",
    "orderStates": {
      "9000000001": { "status": "FILLED", "remainingQuantity": "0" },
      "9000000002": { "status": "PARTIALLY_FILLED", "remainingQuantity": "50" }
    },
    "settlementRequired": true
  }
}
```

---

## 4. 查询订单

- 方法/路径：`GET /{orderId}`
- Summary：查询订单
- Description：根据订单ID查询订单详情

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| orderId | long | 订单ID |

### 响应 data（`OrderDTO`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| orderId | string | 订单ID |
| userId | string | 用户ID |
| marketId | string | 市场ID |
| outcome | string | 结果：`YES` / `NO` |
| side | string | 方向：`BUY` / `SELL` |
| orderType | string | 订单类型：`LIMIT` / `MARKET` |
| price | number | 价格 |
| quantity | number | 原始数量 |
| remainingQuantity | number | 剩余可成交数量 |
| status | string | 状态：`OPEN` / `MATCHING` / `PARTIALLY_FILLED` / `FILLED` / `CANCEL_REQUESTED` / `CANCELLED` / `EXPIRED` / `REJECTED` / `FAILED` |
| timeInForce | string | 有效期：`GTC` / `IOC` / `FOK` / `GTD` |
| expireAt | timestamp（epoch ms） | 过期时间 |
| rejectReason | string | 拒绝原因 |
| idempotencyKey | string | 幂等键 |
| version | integer | 乐观锁版本 |
| createTime | timestamp（epoch ms） | 创建时间 |
| updateTime | timestamp（epoch ms） | 更新时间 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderId": "9000000001",
    "userId": "10001",
    "marketId": "1001",
    "outcome": "YES",
    "side": "BUY",
    "orderType": "LIMIT",
    "price": "0.65",
    "quantity": "100",
    "remainingQuantity": "50",
    "status": "PARTIALLY_FILLED",
    "timeInForce": "GTC",
    "expireAt": null,
    "rejectReason": null,
    "idempotencyKey": "order-abc-20260101",
    "version": 2,
    "createTime": 1772326800000,
    "updateTime": 1772330400000
  }
}
```

---

## 5. 查询成交记录

- 方法/路径：`GET /trades`
- Summary：查询成交记录
- Description：根据订单ID查询该订单（作为 Taker 或 Maker）的所有成交记录

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| orderId | long | 是 | - | 订单ID |

### 响应 data（`List<OrderFillDTO>`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tradeId | string | 成交ID |
| matchId | string | 撮合ID |
| takerOrderId | string | Taker 订单ID |
| makerOrderId | string | Maker 订单ID |
| price | number | 成交价格 |
| quantity | number | 成交数量 |
| fee | number | 手续费 |
| createTime | timestamp（epoch ms） | 创建时间 |
| createBy | string | 创建人 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "tradeId": "8000000001",
      "matchId": "match-20260101-001",
      "takerOrderId": "9000000002",
      "makerOrderId": "9000000001",
      "price": "0.65",
      "quantity": "50",
      "fee": "0.001",
      "createTime": 1772330400000,
      "createBy": "system"
    }
  ]
}
```
