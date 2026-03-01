# order 模块接口摘要

- 模块：`order`

## 请求摘要

| 路径 | 方法 | 功能说明 | 文档 |
| --- | --- | --- | --- |
| `/market` | POST | 创建市场 | [`MarketController@市场管理.md`](./MarketController@市场管理.md) |
| `/market/{marketId}` | GET | 查询市场 | [`MarketController@市场管理.md`](./MarketController@市场管理.md) |
| `/market/active` | GET | 查询有效市场列表 | [`MarketController@市场管理.md`](./MarketController@市场管理.md) |
| `/market/{marketId}/status` | PATCH | 更新市场状态 | [`MarketController@市场管理.md`](./MarketController@市场管理.md) |
| `/market/{marketId}` | DELETE | 删除市场 | [`MarketController@市场管理.md`](./MarketController@市场管理.md) |
| `/create` | POST | 创建订单 | [`OrderController@订单管理.md`](./OrderController@订单管理.md) |
| `/cancel` | POST | 取消订单 | [`OrderController@订单管理.md`](./OrderController@订单管理.md) |
| `/commit-match` | POST | 提交撮合结果 | [`OrderController@订单管理.md`](./OrderController@订单管理.md) |
| `/{orderId}` | GET | 查询订单 | [`OrderController@订单管理.md`](./OrderController@订单管理.md) |
| `/trades` | GET | 查询成交记录 | [`OrderController@订单管理.md`](./OrderController@订单管理.md) |
