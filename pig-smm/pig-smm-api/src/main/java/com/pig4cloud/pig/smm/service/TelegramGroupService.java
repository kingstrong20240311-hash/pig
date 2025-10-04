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

package com.pig4cloud.pig.smm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.smm.dto.TelegramGroupDTO;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.vo.TelegramGroupVO;

import java.util.List;

/**
 * Telegram群组服务接口
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
public interface TelegramGroupService extends IService<TelegramGroup> {

	/**
	 * 分页查询群组
	 *
	 * @param page 分页对象
	 * @param telegramGroupDTO 查询条件
	 * @return 分页结果
	 */
	IPage<TelegramGroupVO> selectPage(IPage<TelegramGroup> page, TelegramGroupDTO telegramGroupDTO);

	/**
	 * 根据优先级分值查询群组
	 *
	 * @param limit 限制数量
	 * @return 群组列表
	 */
	List<TelegramGroupVO> selectByPriorityScore(Integer limit);

	/**
	 * 同步第三方平台群组数据
	 *
	 * @return 同步结果
	 */
	R<String> syncGroupsFromThirdParty();

	/**
	 * 更新群组加群统计
	 *
	 * @param groupId 群组ID
	 * @param success 是否成功
	 */
	void updateJoinStats(String groupId, Boolean success);

	/**
	 * 更新群组发送统计
	 *
	 * @param groupId 群组ID
	 * @param success 是否成功
	 */
	void updateSendStats(String groupId, Boolean success);

}