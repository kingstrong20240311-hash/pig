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
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Telegram账号Mapper
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Mapper
public interface TelegramAccountMapper extends BaseMapper<TelegramAccount> {

	/**
	 * 查询可用且不忙碌的账号
	 *
	 * @return 可用账号列表
	 */
	@Select("SELECT * FROM telegram_account WHERE is_available = 1 AND is_busy = 0")
	List<TelegramAccount> selectAvailableAccounts();

	/**
	 * 根据用户名查询账号
	 *
	 * @param username 用户名
	 * @return 账号信息
	 */
	@Select("SELECT * FROM smm_telegram_account WHERE username = #{username}")
	TelegramAccount selectByUsername(@Param("username") String username);

	/**
	 * 根据第三方账户ID查询账号
	 *
	 * @param thirdPartyAccountId 第三方账户ID
	 * @return 账号信息
	 */
	@Select("SELECT * FROM smm_telegram_account WHERE third_party_account_id = #{thirdPartyAccountId}")
	TelegramAccount selectByThirdPartyAccountId(@Param("thirdPartyAccountId") Long thirdPartyAccountId);

	/**
	 * 根据TG ID查询账号
	 *
	 * @param tgId TG ID
	 * @return 账号信息
	 */
	@Select("SELECT * FROM smm_telegram_account WHERE tg_id = #{tgId}")
	TelegramAccount selectByTgId(@Param("tgId") String tgId);

	/**
	 * 更新账号忙碌状态
	 *
	 * @param id 账号ID
	 * @param isBusy 是否忙碌
	 */
	@Update("UPDATE smm_telegram_account SET is_busy = #{isBusy}, update_time = NOW() WHERE id = #{id}")
	void updateBusyStatus(@Param("id") Long id, @Param("isBusy") Boolean isBusy);

	/**
	 * 更新账号状态
	 *
	 * @param id 账号ID
	 * @param isAvailable 是否可用
	 * @param statusReason 状态原因
	 */
	@Update("UPDATE smm_telegram_account SET is_available = #{isAvailable}, status_reason = #{statusReason}, " +
			"last_check_time = NOW(), update_time = NOW() WHERE id = #{id}")
	void updateAccountStatus(@Param("id") Long id, @Param("isAvailable") Boolean isAvailable,
							@Param("statusReason") String statusReason);

}