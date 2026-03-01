# InBody体测管理（InbodyTestController）

- Controller：`InbodyTestController`
- Tag：`InBody体测管理`
- Base Path：`${gym.api-prefix:}/inbody-test`（默认前缀为空）
- 鉴权：需要 Bearer Token

## 1. 分页查询InBody体测记录

- 方法/路径：`GET /inbody-test/page`
- Summary：分页查询InBody体测记录
- Description：分页查询

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| current | long | 否 | 1 | 当前页 |
| size | long | 否 | 10 | 每页条数 |
| memberId | long | 否 | - | 会员ID |
| coachId | long | 否 | - | 教练ID |

### 响应 data（`Page<InbodyTest>`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| records | array | 当前页数据 |
| total | long | 总记录数 |
| size | long | 每页条数 |
| current | long | 当前页 |
| pages | long | 总页数 |

`records[]` 字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 主键 |
| memberId | long | 会员ID |
| coachId | long | 教练ID |
| testDate | timestamp（epoch ms） | 测试日期 |
| heightCm | number | 身高(cm) |
| weightKg | number | 体重(kg) |
| bmi | number | BMI体质指数 |
| bodyFatPercentage | number | 体脂率(%) |
| bodyFatMassKg | number | 体脂量(kg) |
| skeletalMuscleMassKg | number | 骨骼肌量(kg) |
| leanBodyMassKg | number | 去脂体重(kg) |
| totalBodyWaterKg | number | 体内水分总量(kg) |
| intracellularWaterKg | number | 细胞内水分(kg) |
| extracellularWaterKg | number | 细胞外水分(kg) |
| ecwTbwRatio | number | 水肿指数(ECW/TBW) |
| proteinKg | number | 蛋白质(kg) |
| mineralsKg | number | 无机盐(kg) |
| boneMineralContentKg | number | 骨矿物质含量(kg) |
| basalMetabolicRateKcal | integer | 基础代谢量(kcal) |
| visceralFatLevel | integer | 内脏脂肪等级 |
| waistHipRatio | number | 腰臀比 |
| leanMassRightArmKg | number | 右手臂去脂体重(kg) |
| leanMassLeftArmKg | number | 左手臂去脂体重(kg) |
| leanMassTrunkKg | number | 躯干去脂体重(kg) |
| leanMassRightLegKg | number | 右腿去脂体重(kg) |
| leanMassLeftLegKg | number | 左腿去脂体重(kg) |
| fatMassRightArmKg | number | 右手臂体脂(kg) |
| fatMassLeftArmKg | number | 左手臂体脂(kg) |
| fatMassTrunkKg | number | 躯干体脂(kg) |
| fatMassRightLegKg | number | 右腿体脂(kg) |
| fatMassLeftLegKg | number | 左腿体脂(kg) |
| remark | string | 备注 |
| createBy | string | 创建人 |
| createTime | timestamp（epoch ms） | 创建时间 |
| updateBy | string | 更新人 |
| updateTime | timestamp（epoch ms） | 更新时间 |
| delFlag | string | 删除标识，`0` 正常，`1` 删除 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 10001,
        "memberId": 20001,
        "coachId": 30001,
        "testDate": 1772330400000,
        "heightCm": 175.0,
        "weightKg": 72.3,
        "bmi": 23.6,
        "bodyFatPercentage": 18.2,
        "remark": "整体状态稳定",
        "createBy": "1000",
        "createTime": 1772326800000,
        "updateBy": "1000",
        "updateTime": 1772326800000,
        "delFlag": "0"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

## 2. 通过id查询InBody体测记录

- 方法/路径：`GET /inbody-test/details/{id}`
- Summary：通过id查询InBody体测记录
- Description：通过id查询InBody体测记录

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| id | long | InBody体测记录ID |

### 响应 data（`InbodyTest`）

响应字段同「分页查询InBody体测记录」的 `records[]` 字段。

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10001,
    "memberId": 20001,
    "coachId": 30001,
    "testDate": 1772330400000,
    "heightCm": 175.0,
    "weightKg": 72.3,
    "bmi": 23.6,
    "bodyFatPercentage": 18.2,
    "remark": "整体状态稳定",
    "createBy": "1000",
    "createTime": 1772326800000,
    "updateBy": "1000",
    "updateTime": 1772326800000,
    "delFlag": "0"
  }
}
```

## 3. 新增InBody体测记录

- 方法/路径：`POST /inbody-test`
- Summary：新增InBody体测记录
- Description：新增InBody体测记录
- 权限：`gym_inbody_test_add`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| memberId | long | 是 | 会员ID |
| coachId | long | 否 | 教练ID |
| testDate | timestamp（epoch ms） | 是 | 测试日期 |
| weightKg | number | 是 | 体重(kg) |
| heightCm | number | 否 | 身高(cm) |
| bmi | number | 否 | BMI体质指数 |
| bodyFatPercentage | number | 否 | 体脂率(%) |
| remark | string | 否 | 备注 |

请求示例：

```json
{
  "memberId": 20001,
  "coachId": 30001,
  "testDate": 1772330400000,
  "weightKg": 72.3,
  "heightCm": 175.0,
  "bodyFatPercentage": 18.2,
  "remark": "月度复测"
}
```

### 响应 data（`boolean`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | boolean | 是否新增成功 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

## 4. 修改InBody体测记录

- 方法/路径：`PUT /inbody-test`
- Summary：修改InBody体测记录
- Description：修改InBody体测记录
- 权限：`gym_inbody_test_edit`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 主键 |
| memberId | long | 否 | 会员ID |
| coachId | long | 否 | 教练ID |
| testDate | timestamp（epoch ms） | 否 | 测试日期 |
| weightKg | number | 否 | 体重(kg) |
| remark | string | 否 | 备注 |

请求示例：

```json
{
  "id": 10001,
  "weightKg": 71.8,
  "remark": "饮食调整后体脂下降"
}
```

### 响应 data（`boolean`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | boolean | 是否修改成功 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

## 5. 删除InBody体测记录

- 方法/路径：`DELETE /inbody-test`
- Summary：删除InBody体测记录
- Description：删除InBody体测记录
- 权限：`gym_inbody_test_del`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| ids | array | 是 | 要删除的记录ID数组 |

请求示例：

```json
[10001, 10002]
```

### 响应 data（`boolean`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | boolean | 是否删除成功 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": true
}
```

## 6. 导出InBody体测记录

- 方法/路径：`GET /inbody-test/export`
- Summary：导出InBody体测记录
- Description：导出InBody体测记录
- 权限：`gym_inbody_test_export`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| memberId | long | 否 | - | 会员ID |
| coachId | long | 否 | - | 教练ID |

### 响应

接口返回 Excel 文件流，内容为 `InbodyTest` 列表。
