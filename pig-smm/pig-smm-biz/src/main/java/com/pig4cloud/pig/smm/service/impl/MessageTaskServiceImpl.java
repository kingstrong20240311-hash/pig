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

package com.pig4cloud.pig.smm.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.smm.dto.MessageTaskDTO;
import com.pig4cloud.pig.smm.entity.MessageTask;
import com.pig4cloud.pig.smm.mapper.MessageTaskMapper;
import com.pig4cloud.pig.smm.service.MessageTaskService;
import com.pig4cloud.pig.smm.vo.MessageTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息任务服务实现类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTaskServiceImpl extends ServiceImpl<MessageTaskMapper, MessageTask> implements MessageTaskService {

	@Override
	public IPage<MessageTaskVO> selectPage(IPage<MessageTask> page, MessageTaskDTO messageTaskDTO) {
		// TODO: 实现分页查询逻辑
		return null;
	}

	@Override
	public MessageTaskVO getTaskById(Long id) {
		MessageTask messageTask = this.getById(id);
		if (messageTask == null) {
			return null;
		}

		MessageTaskVO vo = new MessageTaskVO();
		vo.setId(messageTask.getId());
		vo.setThirdPartyTaskId(messageTask.getThirdPartyTaskId());
		vo.setTaskName(messageTask.getTaskName());
		vo.setMessageContent(messageTask.getMessageContent());
		vo.setTaskStatus(messageTask.getTaskStatus());
		vo.setSuccessCount(messageTask.getSuccessCount());
		vo.setFailureCount(messageTask.getFailureCount());
		vo.setCreateTime(messageTask.getCreateTime());
		vo.setStartTime(messageTask.getStartTime());
		vo.setEndTime(messageTask.getEndTime());
		vo.setCreatedBy(messageTask.getCreatedBy());

		return vo;
	}

}