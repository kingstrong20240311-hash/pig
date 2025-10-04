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
import com.pig4cloud.pig.smm.dto.MessageTaskDTO;
import com.pig4cloud.pig.smm.entity.MessageTask;
import com.pig4cloud.pig.smm.vo.MessageTaskVO;

/**
 * 消息任务服务接口
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
public interface MessageTaskService extends IService<MessageTask> {

	/**
	 * 分页查询消息任务
	 *
	 * @param page 分页对象
	 * @param messageTaskDTO 查询条件
	 * @return 分页结果
	 */
	IPage<MessageTaskVO> selectPage(IPage<MessageTask> page, MessageTaskDTO messageTaskDTO);

	/**
	 * 根据ID查询消息任务详情
	 *
	 * @param id 任务ID
	 * @return 任务详情
	 */
	MessageTaskVO getTaskById(Long id);

}