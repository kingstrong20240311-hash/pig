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
import com.pig4cloud.pig.smm.dto.JoinTaskDTO;
import com.pig4cloud.pig.smm.entity.JoinTask;
import com.pig4cloud.pig.smm.mapper.JoinTaskMapper;
import com.pig4cloud.pig.smm.service.JoinTaskService;
import com.pig4cloud.pig.smm.vo.JoinTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 加群任务服务实现类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JoinTaskServiceImpl extends ServiceImpl<JoinTaskMapper, JoinTask> implements JoinTaskService {

	@Override
	public IPage<JoinTaskVO> selectPage(IPage<JoinTask> page, JoinTaskDTO joinTaskDTO) {
		// TODO: 实现分页查询逻辑
		return null;
	}

	@Override
	public JoinTaskVO getTaskById(Long id) {
		JoinTask joinTask = this.getById(id);
		if (joinTask == null) {
			return null;
		}

		JoinTaskVO vo = new JoinTaskVO();
		vo.setId(joinTask.getId());
		vo.setThirdPartyTaskId(joinTask.getThirdPartyTaskId());
		vo.setTaskName(joinTask.getAccountNickname());
		vo.setTaskStatus(joinTask.getStatus());
		vo.setCreateTime(joinTask.getCreateTime());

		return vo;
	}

}
