# 训练动作素材管理（TrainingExerciseMediaController）

- Controller：`TrainingExerciseMediaController`
- Tag：`训练动作素材管理`
- Base Path：`${gym.api-prefix:}/trainingexercisemedia`（默认前缀为空）
- 鉴权：需要 Bearer Token

## 1. 分页查询训练动作素材

- 方法/路径：`GET /trainingexercisemedia/page`
- Summary：分页查询训练动作素材
- Description：分页查询

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| current | long | 否 | 1 | 当前页 |
| size | long | 否 | 10 | 每页条数 |
| sessionId | long | 否 | - | 训练课程ID |
| exerciseRecordId | long | 否 | - | 动作记录ID |

### 响应 data（`Page<TrainingExerciseMedia>`）

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
| sessionId | long | 训练课程ID |
| exerciseRecordId | long | 动作记录ID |
| detailUrl | string | 动作素材详情地址 |
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
        "id": 101,
        "sessionId": 10001,
        "exerciseRecordId": 20001,
        "detailUrl": "https://cdn.example.com/gym/exercise/101.mp4",
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

## 2. 通过id查询训练动作素材

- 方法/路径：`GET /trainingexercisemedia/details/{id}`
- Summary：通过id查询训练动作素材
- Description：通过id查询训练动作素材

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 训练动作素材ID |

### 响应 data（`TrainingExerciseMedia`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 主键 |
| sessionId | long | 训练课程ID |
| exerciseRecordId | long | 动作记录ID |
| detailUrl | string | 动作素材详情地址 |
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
    "id": 101,
    "sessionId": 10001,
    "exerciseRecordId": 20001,
    "detailUrl": "https://cdn.example.com/gym/exercise/101.mp4",
    "createBy": "1000",
    "createTime": 1772326800000,
    "updateBy": "1000",
    "updateTime": 1772326900000,
    "delFlag": "0"
  }
}
```

## 3. 新增训练动作素材

- 方法/路径：`POST /trainingexercisemedia`
- Summary：新增训练动作素材
- Description：新增训练动作素材
- 权限：`gym_trainingexercisemedia_add`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | long | 是 | 训练课程ID |
| exerciseRecordId | long | 是 | 动作记录ID |
| detailUrl | string | 是 | 动作素材详情地址 |

请求示例：

```json
{
  "sessionId": 10001,
  "exerciseRecordId": 20001,
  "detailUrl": "https://cdn.example.com/gym/exercise/101.mp4"
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

## 4. 修改训练动作素材

- 方法/路径：`PUT /trainingexercisemedia`
- Summary：修改训练动作素材
- Description：修改训练动作素材
- 权限：`gym_trainingexercisemedia_edit`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 主键 |
| sessionId | long | 否 | 训练课程ID |
| exerciseRecordId | long | 否 | 动作记录ID |
| detailUrl | string | 否 | 动作素材详情地址 |

请求示例：

```json
{
  "id": 101,
  "detailUrl": "https://cdn.example.com/gym/exercise/101-v2.mp4"
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

## 5. 删除训练动作素材

- 方法/路径：`DELETE /trainingexercisemedia`
- Summary：删除训练动作素材
- Description：删除训练动作素材
- 权限：`gym_trainingexercisemedia_del`

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| ids | array | 是 | 要删除的素材ID数组 |

请求示例：

```json
[101, 102]
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

## 6. 导出训练动作素材

- 方法/路径：`GET /trainingexercisemedia/export`
- Summary：导出训练动作素材
- Description：导出训练动作素材
- 权限：`gym_trainingexercisemedia_export`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| sessionId | long | 否 | - | 训练课程ID |
| exerciseRecordId | long | 否 | - | 动作记录ID |

### 响应

接口返回 Excel 文件流，内容为 `TrainingExerciseMedia` 列表。
