# vault 模块接口摘要

- 模块：`vault`

## 请求摘要

| 路径 | 方法 | 功能说明 | 文档 |
| --- | --- | --- | --- |
| `/asset` | POST | 创建资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset` | PUT | 更新资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset/{assetId}` | GET | 根据ID查询资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset/symbol/{symbol}` | GET | 根据符号查询资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset/list` | GET | 查询所有资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset/list/active` | GET | 查询启用资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset/{assetId}/deactivate` | POST | 停用资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/asset/{assetId}/activate` | POST | 启用资产 | [`VaultAssetController@Vault Asset Management.md`](./VaultAssetController@Vault Asset Management.md) |
| `/freeze/create` | POST | 创建冻结 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/freeze/release` | POST | 释放冻结（内部接口） | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/freeze/claim` | POST | 认领冻结 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/freeze/consume` | POST | 消耗冻结 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/balance` | GET | 查询余额 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/freeze` | GET | 查询冻结记录 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/balance/me` | GET | 查询当前用户余额 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
| `/deposit` | POST | 充值 | [`VaultController@Vault Management.md`](./VaultController@Vault Management.md) |
