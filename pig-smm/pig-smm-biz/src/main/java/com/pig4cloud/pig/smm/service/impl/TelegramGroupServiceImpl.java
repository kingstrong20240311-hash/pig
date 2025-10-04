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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.smm.dto.TelegramGroupDTO;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.gateway.TelegramGateway;
import com.pig4cloud.pig.smm.mapper.TelegramGroupMapper;
import com.pig4cloud.pig.smm.service.TelegramGroupService;
import com.pig4cloud.pig.smm.vo.TelegramGroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Telegram群组服务实现类
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramGroupServiceImpl extends ServiceImpl<TelegramGroupMapper, TelegramGroup> implements TelegramGroupService {

	private final TelegramGateway telegramGateway;

	@Override
	public IPage<TelegramGroupVO> selectPage(IPage<TelegramGroup> page, TelegramGroupDTO telegramGroupDTO) {
		// TODO: 实现分页查询逻辑
		return null;
	}

	@Override
	public List<TelegramGroupVO> selectByPriorityScore(Integer limit) {
		// TODO: 转换为VO
		return null;
	}

	@Override
	public R<String> syncGroupsFromThirdParty() {
		try {
			log.info("开始同步第三方平台群组数据");

			int pageIndex = 1;
			int pageSize = 100;
			int totalSynced = 0;
			int totalInserted = 0;
			int totalUpdated = 0;

			while (true) {
				// 调用第三方API获取群组数据
				R<List<TelegramGroup>> result = telegramGateway.queryGroups(pageIndex, pageSize);

				if (result.getCode() != 0 || result.getData() == null || result.getData().isEmpty()) {
					log.info("第{}页没有更多数据，同步完成", pageIndex);
					break;
				}

				List<TelegramGroup> groups = result.getData();
				log.info("获取到第{}页数据，共{}条群组记录", pageIndex, groups.size());

				// 处理每个群组
				for (TelegramGroup group : groups) {
					if (group.getThirdPartyAccountId() == null) {
						log.warn("群组{}的thirdPartyAccountId为空，跳过处理", group.getGroupName());
						continue;
					}

					// 根据thirdPartyAccountId查询数据库中是否存在
					LambdaQueryWrapper<TelegramGroup> queryWrapper = new LambdaQueryWrapper<>();
					queryWrapper.eq(TelegramGroup::getThirdPartyAccountId, group.getThirdPartyAccountId());
					TelegramGroup existingGroup = this.getOne(queryWrapper);

					if (existingGroup == null) {
						// 不存在则插入
						this.save(group);
						totalInserted++;
						log.debug("新增群组: {} (thirdPartyAccountId: {})", group.getGroupName(), group.getThirdPartyAccountId());
					} else {
						// 存在则更新
						group.setId(existingGroup.getId());
						group.setCreateTime(existingGroup.getCreateTime());
						group.setUpdateTime(null); // 让MyBatis-Plus自动设置更新时间
						this.updateById(group);
						totalUpdated++;
						log.debug("更新群组: {} (thirdPartyAccountId: {})", group.getGroupName(), group.getThirdPartyAccountId());
					}

					totalSynced++;
				}

				// 如果返回的数据少于pageSize，说明已经是最后一页
				if (groups.size() < pageSize) {
					log.info("已到达最后一页，同步完成");
					break;
				}

				pageIndex++;
			}

			String message = String.format("同步完成！总共处理%d条记录，新增%d条，更新%d条",
				totalSynced, totalInserted, totalUpdated);
			log.info(message);
			return R.ok(message);

		} catch (Exception e) {
			log.error("同步第三方平台群组数据异常", e);
			return R.failed("同步失败: " + e.getMessage());
		}
	}

	@Override
	public void updateJoinStats(String groupId, Boolean success) {
		// TODO: 实现统计更新逻辑
	}

	@Override
	public void updateSendStats(String groupId, Boolean success) {
		// TODO: 实现统计更新逻辑
	}

}
