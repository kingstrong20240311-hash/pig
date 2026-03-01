# Vault Management（VaultController）

- Controller：`VaultController`
- Tag：`Vault Management`
- Base Path：（无，方法路径即完整路径）
- 鉴权：需要 Bearer Token

## 1. Create Freeze（创建冻结）

- 方法/路径：`POST /freeze/create`
- Summary：Create Freeze
- Description：冻结用户资产（从可用余额转入冻结余额）

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 用户ID |
| symbol | string | 是 | 资产符号（如 `USDC`） |
| amount | number | 是 | 冻结金额（必须大于0） |
| refType | string | 是 | 引用类型：`ORDER` / `SETTLEMENT` / `DEPOSIT` / `WITHDRAW` / `TRANSFER` / `ADJUSTMENT` / `SYSTEM` |
| refId | string | 是 | 引用ID（如订单ID） |

请求示例：

```json
{
  "userId": 10001,
  "symbol": "USDC",
  "amount": "65.00",
  "refType": "ORDER",
  "refId": "9000000001"
}
```

### 响应 data（`FreezeResponse`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| freezeId | string | 冻结记录ID |
| status | string | 冻结状态：`HELD` / `CLAIMED` / `RELEASED` / `CONSUMED` / `CANCELED` / `EXPIRED` |
| amount | number | 冻结金额 |
| claimTime | timestamp（epoch ms） | 认领时间（仅 claim 操作有意义） |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "freezeId": "7000000001",
    "status": "HELD",
    "amount": "65.00",
    "claimTime": null
  }
}
```

---

## 2. Release Freeze（释放冻结）

- 方法/路径：`POST /freeze/release`
- Summary：Release Freeze
- Description：释放冻结资产（冻结余额转回可用余额）。标注 `@Inner`，**仅限内部服务调用**。

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| refType | string | 是 | 引用类型（同 Create Freeze） |
| refId | string | 是 | 引用ID |
| amount | number | 否 | 释放金额；未传则释放全部冻结金额 |

请求示例：

```json
{
  "refType": "ORDER",
  "refId": "9000000001"
}
```

### 响应 data（`FreezeResponse`）

响应结构同「Create Freeze」，`data.status` 为 `RELEASED`。

---

## 3. Claim Freeze（认领冻结）

- 方法/路径：`POST /freeze/claim`
- Summary：Claim Freeze
- Description：认领冻结资产（标记为结算进行中）

### 请求体

同 Release Freeze。

请求示例：

```json
{
  "refType": "ORDER",
  "refId": "9000000001"
}
```

### 响应 data（`FreezeResponse`）

响应结构同「Create Freeze」，`data.status` 为 `CLAIMED`，`data.claimTime` 有值。

---

## 4. Consume Freeze（消耗冻结）

- 方法/路径：`POST /freeze/consume`
- Summary：Consume Freeze
- Description：消耗冻结资产（资金实际支出，总余额减少）

### 请求体

同 Release Freeze。

请求示例：

```json
{
  "refType": "ORDER",
  "refId": "9000000001"
}
```

### 响应 data（`FreezeResponse`）

响应结构同「Create Freeze」，`data.status` 为 `CONSUMED`。

---

## 5. Get Balance（查询余额）

- 方法/路径：`GET /balance`
- Summary：Get Balance
- Description：根据账户ID和资产符号查询余额

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| accountId | long | 是 | - | 账户ID |
| symbol | string | 是 | - | 资产符号（如 `USDC`） |

### 响应 data（`BalanceResponse`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| balanceId | string | 余额记录ID |
| accountId | string | 账户ID |
| assetId | string | 资产ID |
| symbol | string | 资产符号 |
| available | number | 可用余额 |
| frozen | number | 冻结余额 |
| updateTime | timestamp（epoch ms） | 更新时间 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "balanceId": "6000000001",
    "accountId": "10001",
    "assetId": "1",
    "symbol": "USDC",
    "available": "935.00",
    "frozen": "65.00",
    "updateTime": 1772330400000
  }
}
```

---

## 6. Get Freeze（查询冻结）

- 方法/路径：`GET /freeze`
- Summary：Get Freeze
- Description：根据引用ID和引用类型查询冻结记录

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| refId | string | 是 | - | 引用ID |
| refType | string | 是 | - | 引用类型（枚举同 Create Freeze） |

### 响应 data（`FreezeDTO`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| freezeId | string | 冻结记录ID |
| accountId | string | 账户ID |
| assetId | string | 资产ID |
| amount | number | 冻结金额 |
| status | string | 冻结状态：`HELD` / `CLAIMED` / `RELEASED` / `CONSUMED` / `CANCELED` / `EXPIRED` |
| refType | string | 引用类型 |
| refId | string | 引用ID |
| version | long | 乐观锁版本 |
| createTime | timestamp（epoch ms） | 创建时间 |
| updateTime | timestamp（epoch ms） | 更新时间 |
| claimTime | timestamp（epoch ms） | 认领时间 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "freezeId": "7000000001",
    "accountId": "10001",
    "assetId": "1",
    "amount": "65.00",
    "status": "HELD",
    "refType": "ORDER",
    "refId": "9000000001",
    "version": 1,
    "createTime": 1772326800000,
    "updateTime": 1772326800000,
    "claimTime": null
  }
}
```

---

## 7. Get My Balance（查询当前用户余额）

- 方法/路径：`GET /balance/me`
- Summary：Get My Balance
- Description：查询当前登录用户指定资产的余额

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| symbol | string | 是 | - | 资产符号（如 `USDC`） |

### 响应 data（`BalanceResponse`）

响应结构同「Get Balance」。

---

## 8. Deposit Funds（充值）

- 方法/路径：`POST /deposit`
- Summary：Deposit Funds
- Description：充值增加可用余额

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| userId | long | 是 | 用户ID |
| symbol | string | 是 | 资产符号（如 `USDC`） |
| amount | number | 是 | 充值金额（最小 0.01） |
| refId | string | 是 | 引用ID（外部交易ID，用于幂等） |

请求示例：

```json
{
  "userId": 10001,
  "symbol": "USDC",
  "amount": "1000.00",
  "refId": "deposit-ext-tx-12345"
}
```

### 响应 data（`BalanceResponse`）

响应结构同「Get Balance」，返回充值后的最新余额。
