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
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.smm.dto.TelegramAccountDTO;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.gateway.TelegramGateway;
import com.pig4cloud.pig.smm.mapper.TelegramAccountMapper;
import com.pig4cloud.pig.smm.service.TelegramAccountService;
import com.pig4cloud.pig.smm.vo.TelegramAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Telegram账号服务实现类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAccountServiceImpl extends ServiceImpl<TelegramAccountMapper, TelegramAccount> implements TelegramAccountService {

	private final TelegramGateway telegramGateway;

	@Override
	public IPage<TelegramAccountVO> selectPage(IPage<TelegramAccount> page, TelegramAccountDTO telegramAccountDTO) {
		// TODO: 实现分页查询逻辑
		return null;
	}

	@Override
	public List<TelegramAccountVO> selectAvailableAccounts() {
		// TODO: 实现转换为VO的逻辑
		return null;
	}

	@Override
	public R<String> syncAccountsFromThirdParty() {
		try {
			log.info("开始同步第三方平台账户数据");

			// 调用第三方API获取账户列表
			R<List<TelegramAccount>> thirdPartyResult = telegramGateway.queryAccounts();
			if (thirdPartyResult.getCode() != 0) {
				log.error("调用第三方API失败: {}", thirdPartyResult.getMsg());
				return R.failed("调用第三方API失败: " + thirdPartyResult.getMsg());
			}

			List<TelegramAccount> thirdPartyAccounts = thirdPartyResult.getData();
			if (thirdPartyAccounts == null || thirdPartyAccounts.isEmpty()) {
				log.info("第三方平台没有账户数据");
				return R.ok("第三方平台没有账户数据");
			}

			int insertCount = 0;
			int updateCount = 0;

			// 遍历第三方账户数据
			for (TelegramAccount thirdPartyAccount : thirdPartyAccounts) {
				if (thirdPartyAccount.getThirdPartyAccountId() == null) {
					log.warn("第三方账户ID为空，跳过: {}", thirdPartyAccount.getNickName());
					continue;
				}

				// 根据第三方账户ID查询本地数据库
				TelegramAccount existingAccount = baseMapper.selectByThirdPartyAccountId(thirdPartyAccount.getThirdPartyAccountId());

				if (existingAccount == null) {
					// 数据库中没有，插入新记录
					thirdPartyAccount.setLastCheckTime(java.time.LocalDateTime.now());
					thirdPartyAccount.setStatusReason("从第三方平台同步");

					this.save(thirdPartyAccount);
					insertCount++;
					log.info("插入新账户: {} (第三方ID: {})", thirdPartyAccount.getNickName(), thirdPartyAccount.getThirdPartyAccountId());
				} else {
					// 数据库中有，更新记录
					existingAccount.setNickName(thirdPartyAccount.getNickName());
					existingAccount.setUsername(thirdPartyAccount.getUsername());
					existingAccount.setPhone(thirdPartyAccount.getPhone());
					existingAccount.setTgId(thirdPartyAccount.getTgId());
					existingAccount.setIsAvailable(thirdPartyAccount.getIsAvailable());
					existingAccount.setLastCheckTime(java.time.LocalDateTime.now());
					existingAccount.setStatusReason("从第三方平台同步更新");

					this.updateById(existingAccount);
					updateCount++;
					log.info("更新账户: {} (第三方ID: {})", existingAccount.getNickName(), existingAccount.getThirdPartyAccountId());
				}
			}

			String resultMsg = String.format("同步完成，共处理 %d 个账户，新增 %d 个，更新 %d 个",
				thirdPartyAccounts.size(), insertCount, updateCount);
			log.info(resultMsg);
			return R.ok(resultMsg);

		} catch (Exception e) {
			log.error("同步第三方平台账户数据异常", e);
			return R.failed("同步失败: " + e.getMessage());
		}
	}

	@Override
	public void updateBusyStatus(Long accountId, Boolean isBusy) {
		baseMapper.updateBusyStatus(accountId, isBusy);
	}

	@Override
	public void updateAccountStatus(Long accountId, Boolean isAvailable, String statusReason) {
		baseMapper.updateAccountStatus(accountId, isAvailable, statusReason);
	}

	@Override
	public void addJoinedGroup(Long accountId, Long groupId) {
		// TODO: 实现添加已加入群组逻辑
	}

	@Override
	public void removeJoinedGroup(Long accountId, Long groupId) {
		// TODO: 实现移除已加入群组逻辑
	}

}
