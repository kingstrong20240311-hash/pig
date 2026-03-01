# 后端接口文档

## 统一响应结构

所有接口统一返回 `R<T>` 结构：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | integer | 业务状态码，`0` 表示成功 |
| msg | string | 状态描述 |
| data | object/array/null | 业务数据 |

## 时间字段规范

- 后端所有时间字段统一以时间戳返回。
- 前端负责对时间戳进行格式化展示。

## 模块索引

| 模块 | 说明 | 文档 |
| --- | --- | --- |
| gym | 健身业务接口 | [`gym/index.md`](./gym/index.md) |
| order | 订单与市场管理接口 | [`order/index.md`](./order/index.md) |
| vault | 资产冻结与余额管理接口 | [`vault/index.md`](./vault/index.md) |
