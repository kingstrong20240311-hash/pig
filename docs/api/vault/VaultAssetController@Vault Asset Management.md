# Vault Asset Management（VaultAssetController）

- Controller：`VaultAssetController`
- Tag：`Vault Asset Management`
- Base Path：`/asset`
- 鉴权：需要 Bearer Token

## 1. Create Asset（创建资产）

- 方法/路径：`POST /asset`
- Summary：Create Asset
- Description：创建新的 Vault 资产

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| symbol | string | 是 | 资产符号（如 `USDC`） |
| currencyId | integer | 否 | 货币ID |
| decimals | integer | 是 | 小数位数（≥ 0） |
| isActive | boolean | 否 | 是否启用（默认 `true`） |

请求示例：

```json
{
  "symbol": "USDC",
  "currencyId": 1,
  "decimals": 6,
  "isActive": true
}
```

### 响应 data（`AssetResponse`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| assetId | string | 资产ID |
| symbol | string | 资产符号 |
| currencyId | integer | 货币ID |
| decimals | integer | 小数位数 |
| isActive | boolean | 是否启用 |
| createTime | timestamp（epoch ms） | 创建时间 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "assetId": "1",
    "symbol": "USDC",
    "currencyId": 1,
    "decimals": 6,
    "isActive": true,
    "createTime": 1772326800000
  }
}
```

---

## 2. Update Asset（更新资产）

- 方法/路径：`PUT /asset`
- Summary：Update Asset
- Description：更新已有 Vault 资产信息

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| assetId | long | 是 | 资产ID |
| decimals | integer | 否 | 小数位数（≥ 0） |
| isActive | boolean | 否 | 是否启用 |

请求示例：

```json
{
  "assetId": 1,
  "decimals": 6,
  "isActive": false
}
```

### 响应 data（`AssetResponse`）

响应结构同「Create Asset」。

---

## 3. Get Asset by ID（根据ID查询资产）

- 方法/路径：`GET /asset/{assetId}`
- Summary：Get Asset by ID
- Description：根据资产ID查询 Vault 资产详情

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| assetId | long | 资产ID |

### 响应 data（`AssetResponse`）

响应结构同「Create Asset」。

---

## 4. Get Asset by Symbol（根据符号查询资产）

- 方法/路径：`GET /asset/symbol/{symbol}`
- Summary：Get Asset by Symbol
- Description：根据资产符号查询 Vault 资产详情

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| symbol | string | 资产符号（如 `USDC`） |

### 响应 data（`AssetResponse`）

响应结构同「Create Asset」。

---

## 5. Get All Assets（查询所有资产）

- 方法/路径：`GET /asset/list`
- Summary：Get All Assets
- Description：查询所有 Vault 资产（含已停用）

### 响应 data（`List<AssetResponse>`）

`data` 为 AssetResponse 数组，结构同「Create Asset」。

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "assetId": "1",
      "symbol": "USDC",
      "currencyId": 1,
      "decimals": 6,
      "isActive": true,
      "createTime": 1772326800000
    }
  ]
}
```

---

## 6. Get Active Assets（查询启用资产）

- 方法/路径：`GET /asset/list/active`
- Summary：Get Active Assets
- Description：查询所有处于启用状态（`isActive=true`）的 Vault 资产

### 响应 data（`List<AssetResponse>`）

响应结构同「Get All Assets」，仅返回 `isActive=true` 的资产。

---

## 7. Deactivate Asset（停用资产）

- 方法/路径：`POST /asset/{assetId}/deactivate`
- Summary：Deactivate Asset
- Description：停用指定 Vault 资产

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| assetId | long | 资产ID |

### 响应

```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

---

## 8. Activate Asset（启用资产）

- 方法/路径：`POST /asset/{assetId}/activate`
- Summary：Activate Asset
- Description：启用指定 Vault 资产

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| assetId | long | 资产ID |

### 响应

```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```
