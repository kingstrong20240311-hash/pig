package com.pig4cloud.pig.smm.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.daemon.quartz.constants.PigQuartzEnum;
import com.pig4cloud.pig.smm.dto.GroupJoinResultDTO;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.enums.JoinResultEnum;
import com.pig4cloud.pig.smm.service.TelegramAccountService;
import com.pig4cloud.pig.smm.service.TelegramGroupService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import com.pig4cloud.pig.smm.gateway.TelegramGateway;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class QueryJoinTaskDetailJob {
    private TelegramGateway telegramGateway;
	private TelegramAccountService telegramAccountService;
	private TelegramGroupService telegramGroupService;

    public String execute() {
		QueryWrapper<TelegramAccount> queryWrapper = new QueryWrapper<>();
		queryWrapper.lambda()
				.select(TelegramAccount::getIsJoining, TelegramAccount::getJoinTaskId, TelegramAccount::getId)
				.eq(TelegramAccount::getIsJoining, true)
				.last("limit 1");
		TelegramAccount telegramAccount = telegramAccountService.getOne(queryWrapper);
		if (telegramAccount == null) {
			return PigQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}

        R<List<GroupJoinResultDTO>> result = telegramGateway.queryJoinGroupTaskDetails(
            telegramAccount.getJoinTaskId(), 1, 100
        );
        if (result.getCode() != 0) {
            log.error("查询加群任务详情失败 {}", result.getMsg());
            return PigQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
        }

        List<GroupJoinResultDTO> groupJoinResultDTOs = result.getData();
        boolean allComplete = true;
        for (GroupJoinResultDTO groupJoinResultDTO : groupJoinResultDTOs) {
            if (groupJoinResultDTO.getResult() == JoinResultEnum.INIT) {
                allComplete = false;
            }

            if (groupJoinResultDTO.getResult() == JoinResultEnum.FAIL) {
                if (groupJoinResultDTO.getErrorMessage().equals("找不到目标群组")) {
                    UpdateWrapper<TelegramGroup> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.lambda()
                            .eq(TelegramGroup::getUsername, groupJoinResultDTO.getUsername())
                            .set(TelegramGroup::getIsJoinable, 0);
                    telegramGroupService.update(updateWrapper);
					log.info("更新群({})为不可发送状态", groupJoinResultDTO.getUsername());
                }
            }
        }

		if (allComplete) {
			TelegramAccount updateJoinStatusById = new TelegramAccount();
			updateJoinStatusById.setId(telegramAccount.getId());
			updateJoinStatusById.setIsJoining(false);
			telegramAccountService.updateById(updateJoinStatusById);
			log.info("更新账号({})状态(isJoining={})", telegramAccount.getUsername(), false);
		}

		return PigQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
    }
}
