# 市场管理（MarketController）

- Controller：`MarketController`
- Tag：`市场管理`
- Base Path：`/market`
- 鉴权：需要 Bearer Token

## 1. 创建市场

- 方法/路径：`POST /market`
- Summary：创建市场
- Description：创建新的预测市场
- 权限：`@pms.hasPermission('market_create')`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 是 | 市场名称 |
| status | string | 是 | 市场状态：`INACTIVE` / `ACTIVE` / `EXPIRED` |
| expireAt | timestamp（epoch ms） | 否 | 过期时间 |

请求示例：

```json
{
  "name": "Will BTC exceed $100k by Q3 2026?",
  "status": "ACTIVE",
  "expireAt": 1751328000000
}
```

### 响应 data（`String`）

返回新建市场的 ID（字符串形式）。

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": "1001"
}
```

---

## 2. 查询市场

- 方法/路径：`GET /market/{marketId}`
- Summary：查询市场
- Description：根据市场ID查询市场详情

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| marketId | long | 市场ID |

### 响应 data（`MarketDTO`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| marketId | string | 市场ID |
| name | string | 市场名称 |
| symbolIdYes | integer | YES 订单簿 symbolId |
| symbolIdNo | integer | NO 订单簿 symbolId |
| status | string | 状态：`INACTIVE` / `ACTIVE` / `EXPIRED` |
| expireAt | timestamp（epoch ms） | 过期时间 |
| createTime | timestamp（epoch ms） | 创建时间 |
| updateTime | timestamp（epoch ms） | 更新时间 |
| delFlag | string | 删除标记 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "marketId": "1001",
    "name": "Will BTC exceed $100k by Q3 2026?",
    "symbolIdYes": 101,
    "symbolIdNo": 102,
    "status": "ACTIVE",
    "expireAt": 1751328000000,
    "createTime": 1772326800000,
    "updateTime": 1772326800000,
    "delFlag": "0"
  }
}
```

---

## 3. 查询有效市场列表

- 方法/路径：`GET /market/active`
- Summary：查询有效市场列表
- Description：查询当前有效（`ACTIVE`）的市场列表，可指定参照时间

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| now | timestamp（epoch ms） | 否 | 服务器当前时间 | 参照时间 |

### 响应 data（`List<MarketDTO>`）

响应结构同「查询市场」，`data` 为 MarketDTO 数组。

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "marketId": "1001",
      "name": "Will BTC exceed $100k by Q3 2026?",
      "symbolIdYes": 101,
      "symbolIdNo": 102,
      "status": "ACTIVE",
      "expireAt": 1751328000000,
      "createTime": 1772326800000,
      "updateTime": 1772326800000,
      "delFlag": "0"
    }
  ]
}
```

---

## 4. 更新市场状态

- 方法/路径：`PATCH /market/{marketId}/status`
- Summary：更新市场状态
- Description：上下架或状态切换
- 权限：`@pms.hasPermission('market_update')`

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| marketId | long | 市场ID |

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | string | 是 | 目标状态：`INACTIVE` / `ACTIVE` / `EXPIRED` |

请求示例：

```json
{
  "status": "INACTIVE"
}
```

### 响应

```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

---

## 5. 删除市场

- 方法/路径：`DELETE /market/{marketId}`
- Summary：删除市场
- Description：逻辑删除市场（更新 `delFlag=1`）
- 权限：`@pms.hasPermission('market_delete')`

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| marketId | long | 市场ID |

### 响应

```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```
