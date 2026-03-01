# 训练课程管理（TrainingSessionController）

- Controller：`TrainingSessionController`
- Tag：`训练课程管理`
- Base Path：`${gym.api-prefix:}/trainingsession`（默认前缀为空）
- 鉴权：需要 Bearer Token

## 1. 分页查询训练课程

- 方法/路径：`GET /trainingsession/page`
- Summary：分页查询训练课程
- Description：分页查询

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| current | long | 否 | 1 | 当前页 |
| size | long | 否 | 10 | 每页条数 |
| memberId | long | 否 | - | 会员ID |
| coachId | long | 否 | - | 教练ID |
| lessonPlanId | long | 否 | - | 备课ID |
| status | string | 否 | - | 课程状态：`SCHEDULED`/`CANCELED`/`COMPLETED` |
| scheduledAt | timestamp（epoch ms） | 否 | - | 预约时间（筛选下限） |

### 响应 data（`Page<TrainingSession>`）

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
| lessonPlanId | long | 备课ID |
| scheduledAt | timestamp（epoch ms） | 预约时间 |
| completedAt | timestamp（epoch ms） | 结课时间 |
| status | string | 课程状态：`SCHEDULED`/`CANCELED`/`COMPLETED` |
| cancelReason | string | 取消原因 |
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
        "id": 1001,
        "memberId": 10001,
        "coachId": 20001,
        "lessonPlanId": 30001,
        "scheduledAt": 1772330400000,
        "completedAt": null,
        "status": "SCHEDULED",
        "cancelReason": null,
        "createBy": "1000",
        "createTime": 1772326800000,
        "updateBy": "1000",
        "updateTime": 1772326900000,
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

## 2. 通过id查询训练课程

- 方法/路径：`GET /trainingsession/details/{id}`
- Summary：通过id查询训练课程
- Description：通过id查询训练课程

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 训练课程ID |

### 响应 data（`TrainingSession`）

响应字段同「分页查询训练课程」的 `records[]` 字段。

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1001,
    "memberId": 10001,
    "coachId": 20001,
    "lessonPlanId": 30001,
    "scheduledAt": 1772330400000,
    "completedAt": null,
    "status": "SCHEDULED",
    "cancelReason": null,
    "createBy": "1000",
    "createTime": 1772326800000,
    "updateBy": "1000",
    "updateTime": 1772326900000,
    "delFlag": "0"
  }
}
```

## 3. 新增训练课程

- 方法/路径：`POST /trainingsession`
- Summary：新增训练课程
- Description：新增训练课程
- 权限：`gym_trainingsession_add`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| memberId | long | 是 | 会员ID |
| coachId | long | 是 | 教练ID |
| lessonPlanId | long | 否 | 备课ID |
| scheduledAt | timestamp（epoch ms） | 是 | 预约时间 |
| completedAt | timestamp（epoch ms） | 否 | 结课时间 |
| status | string | 是 | 课程状态：`SCHEDULED`/`CANCELED`/`COMPLETED` |
| cancelReason | string | 否 | 取消原因 |

请求示例：

```json
{
  "memberId": 10001,
  "coachId": 20001,
  "lessonPlanId": 30001,
  "scheduledAt": 1772330400000,
  "status": "SCHEDULED"
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

## 4. 修改训练课程

- 方法/路径：`PUT /trainingsession`
- Summary：修改训练课程
- Description：修改训练课程
- 权限：`gym_trainingsession_edit`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 主键 |
| memberId | long | 否 | 会员ID |
| coachId | long | 否 | 教练ID |
| lessonPlanId | long | 否 | 备课ID |
| scheduledAt | timestamp（epoch ms） | 否 | 预约时间 |
| completedAt | timestamp（epoch ms） | 否 | 结课时间 |
| status | string | 否 | 课程状态：`SCHEDULED`/`CANCELED`/`COMPLETED` |
| cancelReason | string | 否 | 取消原因 |

请求示例：

```json
{
  "id": 1001,
  "status": "CANCELED",
  "cancelReason": "会员临时请假"
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

## 5. 删除训练课程

- 方法/路径：`DELETE /trainingsession`
- Summary：删除训练课程
- Description：删除训练课程
- 权限：`gym_trainingsession_del`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| ids | array | 是 | 要删除的训练课程ID数组 |

请求示例：

```json
[1001, 1002]
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

## 6. 导出训练课程

- 方法/路径：`GET /trainingsession/export`
- Summary：导出训练课程
- Description：导出训练课程
- 权限：`gym_trainingsession_export`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| memberId | long | 否 | - | 会员ID |
| coachId | long | 否 | - | 教练ID |
| status | string | 否 | - | 课程状态：`SCHEDULED`/`CANCELED`/`COMPLETED` |

### 响应

接口返回 Excel 文件流，内容为 `TrainingSession` 列表。
