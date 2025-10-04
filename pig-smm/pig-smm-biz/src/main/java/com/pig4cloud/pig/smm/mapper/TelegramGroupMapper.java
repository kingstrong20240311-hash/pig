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
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Telegram群组Mapper
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Mapper
public interface TelegramGroupMapper extends BaseMapper<TelegramGroup> {

	/**
	 * 根据优先级分值查询群组列表
	 *
	 * @param limit 限制数量
	 * @return 群组列表
	 */
	@Select("SELECT g.*, " +
			"CASE WHEN g.total_join_count > 0 " +
			"THEN (g.total_join_count - IFNULL(g.join_failure_count, 0)) / g.total_join_count " +
			"ELSE 1.0 END as join_success_rate, " +
			"CASE WHEN g.total_send_count > 0 " +
			"THEN (g.total_send_count - IFNULL(g.send_failure_count, 0)) / g.total_send_count " +
			"ELSE 1.0 END as send_success_rate, " +
			"( " +
			"  CASE WHEN g.total_join_count > 0 " +
			"  THEN (g.total_join_count - IFNULL(g.join_failure_count, 0)) / g.total_join_count " +
			"  ELSE 1.0 END * 0.4 + " +
			"  CASE WHEN g.total_send_count > 0 " +
			"  THEN (g.total_send_count - IFNULL(g.send_failure_count, 0)) / g.total_send_count " +
			"  ELSE 1.0 END * 0.4 + " +
			"  IFNULL(g.member_count, 0) * 0.0001 " +
			") as priority_score " +
			"FROM telegram_group g " +
			"ORDER BY priority_score DESC " +
			"LIMIT #{limit}")
	List<TelegramGroup> selectByPriorityScore(@Param("limit") Integer limit);

	/**
	 * 根据群组ID列表查询
	 *
	 * @param groupIds 群组ID列表
	 * @return 群组列表
	 */
	List<TelegramGroup> selectByGroupIds(@Param("groupIds") List<String> groupIds);

	/**
	 * 更新加群统计
	 *
	 * @param id 群组ID
	 * @param success 是否成功
	 */
	void updateJoinStats(@Param("id") Long id, @Param("success") Boolean success);

	/**
	 * 更新发送统计
	 *
	 * @param id 群组ID
	 * @param success 是否成功
	 */
	void updateSendStats(@Param("id") Long id, @Param("success") Boolean success);

}