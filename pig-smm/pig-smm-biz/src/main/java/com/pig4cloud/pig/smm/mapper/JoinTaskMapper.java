/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pig4cloud.pig.smm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.smm.entity.JoinTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 加群任务Mapper
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Mapper
public interface JoinTaskMapper extends BaseMapper<JoinTask> {

	/**
	 * 查询待执行的任务
	 *
	 * @return 待执行任务列表
	 */
	@Select("SELECT * FROM join_task WHERE task_status = 'PENDING' ORDER BY create_time ASC")
	List<JoinTask> selectPendingTasks();

	/**
	 * 根据第三方任务ID查询
	 *
	 * @param thirdPartyTaskId 第三方任务ID
	 * @return 任务信息
	 */
	@Select("SELECT * FROM join_task WHERE third_party_task_id = #{thirdPartyTaskId}")
	JoinTask selectByThirdPartyTaskId(@Param("thirdPartyTaskId") String thirdPartyTaskId);

	/**
	 * 更新任务状态
	 *
	 * @param id 任务ID
	 * @param taskStatus 任务状态
	 */
	@Update("UPDATE join_task SET task_status = #{taskStatus}, update_time = NOW() WHERE id = #{id}")
	void updateTaskStatus(@Param("id") Long id, @Param("taskStatus") String taskStatus);

	/**
	 * 更新任务开始时间
	 *
	 * @param id 任务ID
	 */
	@Update("UPDATE join_task SET task_status = 'RUNNING', start_time = NOW(), update_time = NOW() WHERE id = #{id}")
	void updateTaskStarted(@Param("id") Long id);

	/**
	 * 更新任务结束
	 *
	 * @param id 任务ID
	 * @param taskStatus 任务状态
	 * @param groups 群组状态JSON
	 */
	@Update("UPDATE join_task SET task_status = #{taskStatus}, end_time = NOW(), " +
			"groups = #{groups}, update_time = NOW() WHERE id = #{id}")
	void updateTaskCompleted(@Param("id") Long id, @Param("taskStatus") String taskStatus,
							@Param("groups") String groups);

}