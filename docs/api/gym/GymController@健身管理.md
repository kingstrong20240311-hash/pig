# 健身管理（GymController）

- Controller：`GymController`
- Tag：`健身管理`
- Base Path：`${gym.api-prefix:}`（默认空）

## 1. 发起训练预约

- 方法/路径：`POST /session/create`
- Summary：发起训练预约
- Description：仅教练发起训练预约

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| memberId | long | 是 | 会员ID |
| coachId | long | 是 | 教练ID |
| lessonPlanId | long | 否 | 备课ID |
| scheduledAt | timestamp（epoch ms） | 是 | 预约时间 |

请求示例：

```json
{
  "memberId": 10001,
  "coachId": 20001,
  "lessonPlanId": 30001,
  "scheduledAt": 1772330400000
}
```

### 响应 data（`TrainingSession`）

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
    "id": 1,
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
    "updateTime": 1772326800000,
    "delFlag": "0"
  }
}
```

## 2. 取消训练预约

- 方法/路径：`POST /session/cancel`
- Summary：取消训练预约
- Description：仅课程教练允许取消

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | long | 是 | 训练课程ID |
| coachId | long | 是 | 教练ID |
| cancelReason | string | 否 | 取消原因 |

请求示例：

```json
{
  "sessionId": 1,
  "coachId": 20001,
  "cancelReason": "会员临时请假"
}
```

### 响应

响应结构同「发起训练预约」，`data.status` 为 `CANCELED`。

## 3. 结课并记录动作

- 方法/路径：`POST /session/complete`
- Summary：结课并记录动作
- Description：动作重量/次数/组数必须完整

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | long | 是 | 训练课程ID |
| coachId | long | 是 | 教练ID |
| completedAt | timestamp（epoch ms） | 否 | 结课时间 |
| exerciseRecords | array | 是 | 动作记录列表，至少 1 条 |

`exerciseRecords[]` 字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| exerciseName | string | 是 | 动作名称（非空） |
| weightKg | number | 是 | 重量（kg） |
| reps | integer | 是 | 次数，最小值 1 |
| sets | integer | 是 | 组数，最小值 1 |
| sortOrder | integer | 是 | 排序，最小值 1 |

请求示例：

```json
{
  "sessionId": 1,
  "coachId": 20001,
  "completedAt": 1772334300000,
  "exerciseRecords": [
    {
      "exerciseName": "深蹲",
      "weightKg": 60.0,
      "reps": 8,
      "sets": 4,
      "sortOrder": 1
    }
  ]
}
```

### 响应

响应结构同「发起训练预约」，`data.status` 为 `COMPLETED`。

## 4. 创建 FMS 评估

- 方法/路径：`POST /fms/create`
- Summary：创建FMS评估
- Description：执行FMS评分强校验并返回汇总结果

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| memberId | long | 是 | 会员ID |
| coachId | long | 是 | 教练ID |
| assessmentType | string | 是 | 评估类型：`INITIAL`/`RETEST` |
| versionType | string | 是 | 版本：`OFFICIAL_7`/`SIMPLIFIED` |
| trainingSuggestion | string | 否 | 训练建议 |
| items | array | 是 | FMS动作评分列表，至少 1 条 |

`items[]` 字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| movementType | string | 是 | 动作类型 |
| leftScore | integer | 否 | 左侧分数 |
| rightScore | integer | 否 | 右侧分数 |
| finalScore | integer | 否 | 最终分数 |
| hasClearingTest | boolean | 否 | 是否有清除测试 |
| clearingTestPain | boolean | 否 | 清除测试是否疼痛 |
| painPosition | string | 否 | 疼痛位置 |
| compensationTags | string | 否 | 代偿标签，逗号分隔 |
| remark | string | 否 | 备注 |

`movementType` 枚举值：

- `DEEP_SQUAT`
- `HURDLE_STEP`
- `INLINE_LUNGE`
- `SHOULDER_MOBILITY`
- `ACTIVE_STRAIGHT_LEG_RAISE`
- `TRUNK_STABILITY_PUSHUP`
- `ROTARY_STABILITY`

请求示例：

```json
{
  "memberId": 10001,
  "coachId": 20001,
  "assessmentType": "INITIAL",
  "versionType": "OFFICIAL_7",
  "trainingSuggestion": "优先改善髋关节活动度",
  "items": [
    {
      "movementType": "DEEP_SQUAT",
      "leftScore": 2,
      "rightScore": 2,
      "finalScore": 2,
      "hasClearingTest": true,
      "clearingTestPain": false,
      "painPosition": "",
      "compensationTags": "膝内扣",
      "remark": "下蹲深度不足"
    }
  ]
}
```

### 响应 data（`FmsAssessmentResult`）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| assessmentId | long | 评估ID |
| totalScore | integer | 总分 |
| restrictedMovementCount | integer | 受限动作数量 |
| hasAsymmetry | boolean | 是否存在左右不对称 |
| hasPainRisk | boolean | 是否存在疼痛风险 |

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "assessmentId": 15,
    "totalScore": 14,
    "restrictedMovementCount": 2,
    "hasAsymmetry": true,
    "hasPainRisk": false
  }
}
```
