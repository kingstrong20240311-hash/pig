# gym 模块接口摘要

- 模块：`gym`

## 请求摘要

| 路径 | 方法 | 功能说明 | 文档 |
| --- | --- | --- | --- |
| `/session/create` | POST | 发起训练预约 | [`GymController@健身管理.md`](./GymController@健身管理.md) |
| `/session/cancel` | POST | 取消训练预约 | [`GymController@健身管理.md`](./GymController@健身管理.md) |
| `/session/complete` | POST | 结课并记录动作 | [`GymController@健身管理.md`](./GymController@健身管理.md) |
| `/fms/create` | POST | 创建FMS评估 | [`GymController@健身管理.md`](./GymController@健身管理.md) |
| `/inbody-test/page` | GET | 分页查询InBody体测记录 | [`InbodyTestController@InBody体测管理.md`](./InbodyTestController@InBody体测管理.md) |
| `/inbody-test/details/{id}` | GET | 通过id查询InBody体测记录 | [`InbodyTestController@InBody体测管理.md`](./InbodyTestController@InBody体测管理.md) |
| `/inbody-test` | POST | 新增InBody体测记录 | [`InbodyTestController@InBody体测管理.md`](./InbodyTestController@InBody体测管理.md) |
| `/inbody-test` | PUT | 修改InBody体测记录 | [`InbodyTestController@InBody体测管理.md`](./InbodyTestController@InBody体测管理.md) |
| `/inbody-test` | DELETE | 删除InBody体测记录 | [`InbodyTestController@InBody体测管理.md`](./InbodyTestController@InBody体测管理.md) |
| `/inbody-test/export` | GET | 导出InBody体测记录 | [`InbodyTestController@InBody体测管理.md`](./InbodyTestController@InBody体测管理.md) |
| `/member/page` | GET | 分页查询会员 | [`MemberController@会员管理.md`](./MemberController@会员管理.md) |
| `/member/details/{id}` | GET | 通过id查询会员 | [`MemberController@会员管理.md`](./MemberController@会员管理.md) |
| `/member` | POST | 新增会员 | [`MemberController@会员管理.md`](./MemberController@会员管理.md) |
| `/member` | PUT | 修改会员 | [`MemberController@会员管理.md`](./MemberController@会员管理.md) |
| `/member` | DELETE | 删除会员 | [`MemberController@会员管理.md`](./MemberController@会员管理.md) |
| `/member/export` | GET | 导出会员 | [`MemberController@会员管理.md`](./MemberController@会员管理.md) |
| `/trainingexercisemedia/page` | GET | 分页查询训练动作素材 | [`TrainingExerciseMediaController@训练动作素材管理.md`](./TrainingExerciseMediaController@训练动作素材管理.md) |
| `/trainingexercisemedia/details/{id}` | GET | 通过id查询训练动作素材 | [`TrainingExerciseMediaController@训练动作素材管理.md`](./TrainingExerciseMediaController@训练动作素材管理.md) |
| `/trainingexercisemedia` | POST | 新增训练动作素材 | [`TrainingExerciseMediaController@训练动作素材管理.md`](./TrainingExerciseMediaController@训练动作素材管理.md) |
| `/trainingexercisemedia` | PUT | 修改训练动作素材 | [`TrainingExerciseMediaController@训练动作素材管理.md`](./TrainingExerciseMediaController@训练动作素材管理.md) |
| `/trainingexercisemedia` | DELETE | 删除训练动作素材 | [`TrainingExerciseMediaController@训练动作素材管理.md`](./TrainingExerciseMediaController@训练动作素材管理.md) |
| `/trainingexercisemedia/export` | GET | 导出训练动作素材 | [`TrainingExerciseMediaController@训练动作素材管理.md`](./TrainingExerciseMediaController@训练动作素材管理.md) |
| `/trainingsession/page` | GET | 分页查询训练课程 | [`TrainingSessionController@训练课程管理.md`](./TrainingSessionController@训练课程管理.md) |
| `/trainingsession/details/{id}` | GET | 通过id查询训练课程 | [`TrainingSessionController@训练课程管理.md`](./TrainingSessionController@训练课程管理.md) |
| `/trainingsession` | POST | 新增训练课程 | [`TrainingSessionController@训练课程管理.md`](./TrainingSessionController@训练课程管理.md) |
| `/trainingsession` | PUT | 修改训练课程 | [`TrainingSessionController@训练课程管理.md`](./TrainingSessionController@训练课程管理.md) |
| `/trainingsession` | DELETE | 删除训练课程 | [`TrainingSessionController@训练课程管理.md`](./TrainingSessionController@训练课程管理.md) |
| `/trainingsession/export` | GET | 导出训练课程 | [`TrainingSessionController@训练课程管理.md`](./TrainingSessionController@训练课程管理.md) |
