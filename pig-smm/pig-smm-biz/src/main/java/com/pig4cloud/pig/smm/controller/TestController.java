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

package com.pig4cloud.pig.smm.controller;

import cn.hutool.json.JSONUtil;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.smm.dto.GroupJoinResultDTO;
import com.pig4cloud.pig.smm.entity.JoinTask;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.gateway.TelegramGateway;
import com.pig4cloud.pig.smm.gateway.dto.*;
import com.pig4cloud.pig.common.security.annotation.Inner;
import com.pig4cloud.pig.smm.job.CreateJoinTaskJob;
import com.pig4cloud.pig.smm.job.QueryJoinTaskDetailJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Gateway测试控制器
 *
 * @author pig4cloud
 * @date 2025-09-26
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/smm/test")
@Tag(name = "Gateway测试", description = "Gateway接口测试")
@Inner(value = false)
public class TestController {

	private final TelegramGateway telegramGateway;

	/**
	 * 测试查询账户列表
	 */
	@GetMapping("/accounts")
	@Operation(summary = "测试查询账户列表", description = "测试查询账户列表")
	public R<List<TelegramAccount>> testQueryAccounts() {
		log.info("========== 测试查询账户列表 ==========");
		try {
			return telegramGateway.queryAccounts();
		} catch (Exception e) {
			log.error("测试查询账户列表异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试查询群组列表
	 */
	@GetMapping("/groups")
	@Operation(summary = "测试查询群组列表", description = "测试查询群组列表")
	public R<List<TelegramGroup>> testQueryGroups(@RequestParam(defaultValue = "1") Integer pageIndex,
												  @RequestParam(defaultValue = "20") Integer pageSize) {
		log.info("========== 测试查询群组列表 ==========");
		try {
			return telegramGateway.queryGroups(pageIndex, pageSize);
		} catch (Exception e) {
			log.error("测试查询群组列表异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试查询已加入群组
	 */
	@GetMapping("/joined-groups")
	@Operation(summary = "测试查询已加入群组", description = "测试查询已加入群组")
	public R<List<TelegramGroup>> testQueryJoinedGroups(@RequestParam(defaultValue = "55136") Long tgAccountId) {
		log.info("========== 测试查询已加入群组 ==========");
		try {
			return telegramGateway.queryJoinedGroups(tgAccountId);
		} catch (Exception e) {
			log.error("测试查询已加入群组异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试创建群发任务
	 */
	@PostMapping("/send-task")
	@Operation(summary = "测试创建群发任务", description = "测试创建群发任务")
	public R<String> testCreateSendTask() {
		log.info("========== 测试创建群发任务 ==========");
		try {
			CreateSendTaskRequest request = new CreateSendTaskRequest();
			request.setTgAccountId(55136L);
			request.setText("测试消息 - 大家好，这是一条测试消息");
			request.setTargetGroupList("[{\"title\":\"测试群组\",\"username\":\"test_group\"}]");
			request.setTimeUnit("s");
			request.setTimeSpacing(30);
			request.setLimit(5);

			R<CreateSendTaskResponse> result = telegramGateway.createSendTask(request);
			log.info("创建群发任务结果: {}", JSONUtil.toJsonPrettyStr(result));

			if (result.getCode() == 0 && result.getData() != null) {
				CreateSendTaskResponse data = result.getData();
				log.info("创建的任务信息: {}", JSONUtil.toJsonPrettyStr(data));
			}
			return R.ok(JSONUtil.toJsonPrettyStr(result));
		} catch (Exception e) {
			log.error("测试创建群发任务异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试查询发送记录
	 */
	@GetMapping("/send-record")
	@Operation(summary = "测试查询发送记录", description = "测试查询发送记录")
	public R<String> testQuerySendRecord(
			@RequestParam(defaultValue = "616120") Long taskId,
			@RequestParam(defaultValue = "1") Integer pageIndex,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		log.info("========== 测试查询发送记录 ==========");
		try {
			R<SendRecordResponse> result = telegramGateway.querySendRecord(taskId, pageIndex, pageSize);
			log.info("查询发送记录结果: {}", JSONUtil.toJsonPrettyStr(result));

			if (result.getCode() == 0 && result.getData() != null) {
				SendRecordResponse data = result.getData();
				log.info("发送记录总数: {}, 当前页记录数: {}",
					data.getTotalRecords(),
					data.getRecords() != null ? data.getRecords().size() : 0);
				if (data.getRecords() != null && !data.getRecords().isEmpty()) {
					log.info("第一条发送记录: {}", JSONUtil.toJsonPrettyStr(data.getRecords().get(0)));
				}
			}
			return R.ok(JSONUtil.toJsonPrettyStr(result));
		} catch (Exception e) {
			log.error("测试查询发送记录异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试创建加群任务
	 */
	@PostMapping("/join-task")
	@Operation(summary = "测试创建加群任务", description = "测试创建加群任务")
	public R<String> testCreateJoinGroupTask() {
		log.info("========== 测试创建加群任务 ==========");
		try {
			CreateJoinGroupTaskRequest request = new CreateJoinGroupTaskRequest();
			request.setTaskId(20696L);
			request.setTgAccountId(72670L);
			TelegramGroup group = new TelegramGroup();
			group.setGroupName("苗瓦迪 大队");
			group.setUsername("ammwd0");
			request.setTargetGroupList(Collections.singletonList(group));
			request.setTimeUnit("m");
			request.setTimeSpacing(3);
			request.setLimit(3);

			R<CreateJoinGroupTaskResponse> result = telegramGateway.createJoinGroupTask(request);
			log.info("创建加群任务结果: {}", JSONUtil.toJsonPrettyStr(result));

			if (result.getCode() == 0 && result.getData() != null) {
				CreateJoinGroupTaskResponse data = result.getData();
				log.info("创建的加群任务信息: {}", JSONUtil.toJsonPrettyStr(data));
			}
			return R.ok(JSONUtil.toJsonPrettyStr(result));
		} catch (Exception e) {
			log.error("测试创建加群任务异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试启动加群任务
	 */
	@PostMapping("/start-join-task/{taskId}")
	@Operation(summary = "测试启动加群任务", description = "测试启动加群任务")
	public R<String> testStartJoinGroupTask(@PathVariable Long taskId) {
		log.info("========== 测试启动加群任务 ==========");
		try {
			R<Void> result = telegramGateway.startJoinGroupTask(taskId);
			log.info("启动加群任务结果: {}", JSONUtil.toJsonPrettyStr(result));
			return R.ok(JSONUtil.toJsonPrettyStr(result));
		} catch (Exception e) {
			log.error("测试启动加群任务异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试查询加群任务列表
	 */
	@GetMapping("/join-tasks")
	@Operation(summary = "测试查询加群任务列表", description = "测试查询加群任务列表")
	public R<List<JoinTask>> testQueryJoinGroupTasks() {
		log.info("========== 测试查询加群任务列表 ==========");
		try {
			return telegramGateway.queryJoinGroupTasks();
		} catch (Exception e) {
			log.error("测试查询加群任务列表异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	private final CreateJoinTaskJob createJoinTaskJob;
	private final QueryJoinTaskDetailJob queryJoinTaskDetailJob;

	/**
	 * 测试加群任务
	 */
	@GetMapping("/join-task")
	@Operation(summary = "测试加群任务", description = "测试加群任务")
	public R<Void> testRunJoinGroupTasks() {
		log.info("========== 测试查询加群任务列表 ==========");
		try {
			createJoinTaskJob.execute("1");
			return R.ok();
		} catch (Exception e) {
			log.error("测试查询加群任务列表异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试加群任务
	 */
	@GetMapping("/query-join-task-detail")
	@Operation(summary = "测试查询加群任务详情任务", description = "测试查询加群任务详情任务")
	public R<Void> testQueryJoinGroupDetailTasks() {
		log.info("========== 测试查询加群任务详情任务 ==========");
		try {
			queryJoinTaskDetailJob.execute();
			return R.ok();
		} catch (Exception e) {
			log.error("测试查询加群任务详情任务异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}

	/**
	 * 测试查询加群任务详情
	 */
	@GetMapping("/join-task-details")
	@Operation(summary = "测试查询加群任务详情", description = "测试查询加群任务详情")
	public R<List<GroupJoinResultDTO>> testQueryJoinGroupTaskDetails(
			@RequestParam(defaultValue = "15289") Long taskId,
			@RequestParam(defaultValue = "1") Integer pageIndex,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		log.info("========== 测试查询加群任务详情 ==========");
		try {
			return telegramGateway.queryJoinGroupTaskDetails(taskId, pageIndex, pageSize);
		} catch (Exception e) {
			log.error("测试查询加群任务详情异常", e);
			return R.failed("测试失败: " + e.getMessage());
		}
	}
}
