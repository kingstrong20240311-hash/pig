# 会员管理（MemberController）

- Controller：`MemberController`
- Tag：`会员管理`
- Base Path：`${gym.api-prefix:}/member`（默认前缀为空）
- 鉴权：需要 Bearer Token

## 1. 分页查询会员

- 方法/路径：`GET /member/page`
- Summary：分页查询会员
- Description：分页查询

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| current | long | 否 | 1 | 当前页 |
| size | long | 否 | 10 | 每页条数 |
| name | string | 否 | - | 姓名（模糊查询） |
| mobile | string | 否 | - | 手机号（模糊查询） |
| deptId | long | 否 | - | 所属门店/场馆 |
| coachId | long | 否 | - | 所属教练 |
| enabled | boolean | 否 | - | 是否可继续预约与训练 |

### 响应 data（`Page<Member>`）

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
| deptId | long | 所属门店/场馆 |
| coachId | long | 所属教练 |
| name | string | 姓名 |
| avatarUrl | string | 头像地址 |
| mobile | string | 联系手机号 |
| gender | string | 性别（枚举编码） |
| birthday | string（ISO-8601） | 生日 |
| heightCm | number | 身高（cm） |
| weightKg | number | 体重（kg） |
| injuryHistory | string | 伤病史 |
| medicalNotes | string | 医疗注意事项 |
| goalNotes | string | 训练目标补充 |
| enabled | boolean | 是否可继续预约与训练 |
| fmsScore | integer | FMS评估得分 |
| lastFmsScore | integer | 上次FMS评估得分 |
| lastTrainingAt | timestamp（epoch ms） | 最近训练时间 |
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
        "deptId": 10,
        "coachId": 20001,
        "name": "张三",
        "avatarUrl": "https://cdn.example.com/avatar/10001.jpg",
        "mobile": "13800000000",
        "gender": "MALE",
        "birthday": "1993-05-12",
        "heightCm": 175.0,
        "weightKg": 72.0,
        "enabled": true,
        "fmsScore": 14,
        "lastFmsScore": 13,
        "lastTrainingAt": 1772330400000,
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

## 2. 通过id查询会员

- 方法/路径：`GET /member/details/{id}`
- Summary：通过id查询会员
- Description：通过id查询会员

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 会员ID |

### 响应 data（`Member`）

响应字段同「分页查询会员」的 `records[]` 字段。

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 10001,
    "deptId": 10,
    "coachId": 20001,
    "name": "张三",
    "mobile": "13800000000",
    "enabled": true,
    "createBy": "1000",
    "createTime": 1772326800000,
    "updateBy": "1000",
    "updateTime": 1772326900000,
    "delFlag": "0"
  }
}
```

## 3. 新增会员

- 方法/路径：`POST /member`
- Summary：新增会员
- Description：新增会员
- 权限：`gym_member_add`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| deptId | long | 是 | 所属门店/场馆 |
| coachId | long | 是 | 所属教练 |
| name | string | 是 | 姓名 |
| mobile | string | 是 | 联系手机号 |
| enabled | boolean | 是 | 是否可继续预约与训练 |
| gender | string | 否 | 性别（枚举编码） |
| birthday | string（ISO-8601） | 否 | 生日 |
| heightCm | number | 否 | 身高（cm） |
| weightKg | number | 否 | 体重（kg） |

请求示例：

```json
{
  "deptId": 10,
  "coachId": 20001,
  "name": "张三",
  "mobile": "13800000000",
  "enabled": true,
  "gender": "MALE",
  "birthday": "1993-05-12",
  "heightCm": 175.0,
  "weightKg": 72.0
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

## 4. 修改会员

- 方法/路径：`PUT /member`
- Summary：修改会员
- Description：修改会员
- 权限：`gym_member_edit`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 主键 |
| deptId | long | 否 | 所属门店/场馆 |
| coachId | long | 否 | 所属教练 |
| name | string | 否 | 姓名 |
| mobile | string | 否 | 联系手机号 |
| enabled | boolean | 否 | 是否可继续预约与训练 |

请求示例：

```json
{
  "id": 10001,
  "coachId": 20002,
  "enabled": true,
  "goalNotes": "增强下肢力量"
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

## 5. 删除会员

- 方法/路径：`DELETE /member`
- Summary：删除会员
- Description：删除会员
- 权限：`gym_member_del`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| ids | array | 是 | 要删除的会员ID数组 |

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

## 6. 导出会员

- 方法/路径：`GET /member/export`
- Summary：导出会员
- Description：导出会员
- 权限：`gym_member_export`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| name | string | 否 | - | 姓名（模糊查询） |
| mobile | string | 否 | - | 手机号（模糊查询） |
| deptId | long | 否 | - | 所属门店/场馆 |
| coachId | long | 否 | - | 所属教练 |
| enabled | boolean | 否 | - | 是否可继续预约与训练 |

### 响应

接口返回 Excel 文件流，内容为 `Member` 列表。
