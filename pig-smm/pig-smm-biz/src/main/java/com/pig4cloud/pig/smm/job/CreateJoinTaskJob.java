package com.pig4cloud.pig.smm.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.daemon.quartz.constants.PigQuartzEnum;
import com.pig4cloud.pig.smm.entity.JoinTask;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.gateway.TelegramGateway;
import com.pig4cloud.pig.smm.gateway.dto.CreateJoinGroupTaskRequest;
import com.pig4cloud.pig.smm.service.JoinTaskService;
import com.pig4cloud.pig.smm.service.TelegramAccountService;
import com.pig4cloud.pig.smm.service.TelegramGroupService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Component
public class CreateJoinTaskJob {

	private final TelegramAccountService telegramAccountService;
	private final TelegramGroupService telegramGroupService;
	private final TelegramGateway telegramGateway;
	private final JoinTaskService joinTaskService;

	public String execute(String batchSize) {

		// 选择执行账号
		QueryWrapper<TelegramAccount> queryAccountWrapper = new QueryWrapper<>();
		queryAccountWrapper.lambda()
				.select(TelegramAccount::getThirdPartyAccountId, TelegramAccount::getUsername, TelegramAccount::getId)
				.eq(TelegramAccount::getIsAvailable, true)
				.eq(TelegramAccount::getIsJoining, false)
						.last("limit 1");
		TelegramAccount telegramAccount = telegramAccountService.getOne(queryAccountWrapper);
		if (telegramAccount == null) {
			log.info("没有空闲账号");
			return  PigQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}

		R<List<TelegramGroup>> joinedGroupResult = telegramGateway.queryJoinedGroups(telegramAccount.getThirdPartyAccountId());
		if (joinedGroupResult.getCode() != 0) {
			log.error("查询({})已加入群聊失败:{}", telegramAccount.getThirdPartyAccountId(), joinedGroupResult.getMsg());
			return  PigQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}

		R<List<JoinTask>> joinTaskResult = telegramGateway.queryJoinGroupTasks();
		if (joinTaskResult.getCode() != 0) {
			log.error("查询加群任务失败 {}", joinTaskResult.getMsg());
			return  PigQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}


		// 需要加入的群
		QueryWrapper<TelegramGroup> queryGroupWrapper = new QueryWrapper<>();
		queryGroupWrapper.lambda()
				.gt(TelegramGroup::getSendScore, 0)
				.isNull(TelegramGroup::getSendBy)
				.notIn(TelegramGroup::getUsername, joinedGroupResult.getData().stream().map(TelegramGroup::getUsername).collect(Collectors.toList()))
				.orderByDesc(TelegramGroup::getMemberCount)
				.last("limit " + batchSize);
		List<TelegramGroup> targetGroups = telegramGroupService.list(queryGroupWrapper);
		if (targetGroups == null || targetGroups.isEmpty()) {
			log.info("无未分配群");
			return  PigQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
		}

		// 创建加群任务
		CreateJoinGroupTaskRequest createJoinGroupTaskRequest = new CreateJoinGroupTaskRequest();
		createJoinGroupTaskRequest.setTgAccountId(telegramAccount.getThirdPartyAccountId());
		createJoinGroupTaskRequest.setTimeSpacing(3);
		createJoinGroupTaskRequest.setTimeUnit("m");

		Optional<JoinTask> taskOptional = getJoinTask(telegramAccount.getThirdPartyAccountId(), JoinTask.TaskStatusEnum.FINISH);
		createJoinGroupTaskRequest.setTaskId(taskOptional.map(JoinTask::getThirdPartyTaskId).orElse(null));

		createJoinGroupTaskRequest.setTargetGroupList(targetGroups);
		telegramGateway.createJoinGroupTask(createJoinGroupTaskRequest);

		// 启动加群任务
		Optional<JoinTask> needStartTaskOptional = getJoinTask(telegramAccount.getThirdPartyAccountId(), JoinTask.TaskStatusEnum.READY);
		if (needStartTaskOptional.isEmpty()) {
			log.info(
					"{} 启动启动加群任务失败，群:{}",
					telegramAccount.getUsername(),
					targetGroups.stream()
							.map(group -> String.format("%s(%s)", group.getGroupName(), group.getUsername()))
							.collect(Collectors.joining(","))
			);
			return  PigQuartzEnum.JOB_LOG_STATUS_FAIL.getType();
		}
		JoinTask needStartTask = needStartTaskOptional.get();
		telegramGateway.startJoinGroupTask(needStartTask.getThirdPartyTaskId());

		TelegramAccount updateJoinStatus = new TelegramAccount();
		updateJoinStatus.setId(telegramAccount.getId());
		updateJoinStatus.setIsJoining(true);
		updateJoinStatus.setJoinTaskId(needStartTask.getThirdPartyTaskId());
		telegramAccountService.updateById(updateJoinStatus);

		log.info(
				"{} 启动启动加群任务({})成功，群:{}",
				telegramAccount.getUsername(),
				needStartTask.getThirdPartyTaskId(),
				targetGroups.stream()
						.map(group -> String.format("%s(%s)", group.getGroupName(), group.getUsername()))
						.collect(Collectors.joining(","))
		);
		return  PigQuartzEnum.JOB_LOG_STATUS_SUCCESS.getType();
	}

	private Optional<JoinTask> getJoinTask(Long thirdPartyAccountId, JoinTask.TaskStatusEnum taskStatus) {
		R<List<JoinTask>> joinTaskResult = telegramGateway.queryJoinGroupTasks();
		List<JoinTask>joinTasks = joinTaskResult.getData();
		return joinTasks.stream()
				.filter(
						joinTask ->
								taskStatus != null ?
										joinTask.getTgAccountId().equals(thirdPartyAccountId) &&
												joinTask.getStatus().equals(taskStatus) : joinTask.getTgAccountId().equals(thirdPartyAccountId)
				)
				.findFirst();
	}
}
