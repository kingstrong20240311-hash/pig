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

package com.pig4cloud.pig.smm.gateway;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.smm.dto.GroupJoinResultDTO;
import com.pig4cloud.pig.smm.entity.JoinTask;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.gateway.dto.*;

import java.util.List;

/**
 * FeitApp Telegram API网关接口
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
public interface TelegramGateway {

	/**
	 * 查询账户列表
	 *
	 * @return 账户列表
	 */
	R<List<TelegramAccount>> queryAccounts();

	/**
	 * 查询全局群组列表
	 *
	 * @param pageIndex 页码
	 * @param pageSize 页大小
	 * @return 群组列表
	 */
	R<List<TelegramGroup>> queryGroups(Integer pageIndex, Integer pageSize);

	/**
	 * 查询指定账号已加入的群组
	 *
	 * @param tgAccountId TG账号ID
	 * @return 群组列表
	 */
	R<List<TelegramGroup>> queryJoinedGroups(Long tgAccountId);

	/**
	 * 创建群发任务
	 *
	 * @param request 群发任务请求
	 * @return 任务创建结果
	 */
	R<CreateSendTaskResponse> createSendTask(CreateSendTaskRequest request);

	/**
	 * 查询发送记录
	 *
	 * @param taskId 任务ID
	 * @param pageIndex 页码
	 * @param pageSize 页大小
	 * @return 发送记录
	 */
	R<SendRecordResponse> querySendRecord(Long taskId, Integer pageIndex, Integer pageSize);

	/**
	 * 创建加群任务
	 *
	 * @param request 加群任务请求
	 * @return 任务创建结果
	 */
	R<CreateJoinGroupTaskResponse> createJoinGroupTask(CreateJoinGroupTaskRequest request);

	/**
	 * 启动加群任务
	 *
	 * @param taskId 任务ID
	 * @return 启动结果
	 */
	R<Void> startJoinGroupTask(Long taskId);

	/**
	 * 查询加群任务列表
	 *
	 * @return 任务列表
	 */
	R<List<JoinTask>> queryJoinGroupTasks();

	/**
	 * 查询加群任务详情
	 *
	 * @param taskId 任务ID
	 * @param pageIndex 页码
	 * @param pageSize 页大小
	 * @return 任务详情
	 */
	R<List<GroupJoinResultDTO>> queryJoinGroupTaskDetails(Long taskId, Integer pageIndex, Integer pageSize);

}
