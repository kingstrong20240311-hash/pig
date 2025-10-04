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
import com.pig4cloud.pig.smm.dto.TelegramAccountDTO;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.vo.TelegramAccountVO;

import java.util.List;

/**
 * Telegram账号服务接口
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
public interface TelegramAccountService extends IService<TelegramAccount> {

	/**
	 * 分页查询账号
	 *
	 * @param page 分页对象
	 * @param telegramAccountDTO 查询条件
	 * @return 分页结果
	 */
	IPage<TelegramAccountVO> selectPage(IPage<TelegramAccount> page, TelegramAccountDTO telegramAccountDTO);

	/**
	 * 查询可用且不忙碌的账号
	 *
	 * @return 可用账号列表
	 */
	List<TelegramAccountVO> selectAvailableAccounts();

	/**
	 * 同步第三方平台账户数据
	 *
	 * @return 同步结果
	 */
	R<String> syncAccountsFromThirdParty();

	/**
	 * 更新账号忙碌状态
	 *
	 * @param accountId 账号ID
	 * @param isBusy 是否忙碌
	 */
	void updateBusyStatus(Long accountId, Boolean isBusy);

	/**
	 * 更新账号状态
	 *
	 * @param accountId 账号ID
	 * @param isAvailable 是否可用
	 * @param statusReason 状态原因
	 */
	void updateAccountStatus(Long accountId, Boolean isAvailable, String statusReason);

	/**
	 * 添加账号加入的群组
	 *
	 * @param accountId 账号ID
	 * @param groupId 群组ID
	 */
	void addJoinedGroup(Long accountId, Long groupId);

	/**
	 * 移除账号加入的群组
	 *
	 * @param accountId 账号ID
	 * @param groupId 群组ID
	 */
	void removeJoinedGroup(Long accountId, Long groupId);

}