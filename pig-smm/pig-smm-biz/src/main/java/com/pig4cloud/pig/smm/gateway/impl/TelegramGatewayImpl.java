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

package com.pig4cloud.pig.smm.gateway.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.core.util.RedisUtils;
import com.pig4cloud.pig.smm.dto.GroupJoinResultDTO;
import com.pig4cloud.pig.smm.entity.JoinTask;
import com.pig4cloud.pig.smm.entity.TelegramAccount;
import com.pig4cloud.pig.smm.entity.TelegramGroup;
import com.pig4cloud.pig.smm.enums.JoinResultEnum;
import com.pig4cloud.pig.smm.gateway.TelegramGateway;
import com.pig4cloud.pig.smm.gateway.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FeitApp Telegram API网关实现
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Slf4j
@Component
public class TelegramGatewayImpl implements TelegramGateway {

	@Value("${pig.smm.telegram.gateway.base-url:https://api.feitapp.com/feit-api/v1}")
	private String baseUrl;

	@Value("${pig.smm.telegram.gateway.account}")
	private String account;

	@Value("${pig.smm.telegram.gateway.password}")
	private String password;

	// Redis key 常量
	private static final String TELEGRAM_TOKEN_KEY = "telegram:gateway:token";
	private static final String TELEGRAM_TOKEN_EXPIRE_KEY = "telegram:gateway:token:expire";

	/**
	 * 清理过期的 token 信息
	 */
	private void clearExpiredToken() {
		RedisUtils.delete(TELEGRAM_TOKEN_KEY, TELEGRAM_TOKEN_EXPIRE_KEY);
		log.info("已清理过期的 Telegram token 信息");
	}

	/**
	 * 登录获取token
	 */
	private String login() {
		try {
			// 先清理过期的 token
			clearExpiredToken();

			LoginRequest loginRequest = new LoginRequest();
			loginRequest.setAccount(account);
			loginRequest.setPassword(password);

			HttpResponse response = HttpRequest.post(baseUrl + "/user/login")
					.header("Content-Type", "application/json")
					.body(JSONUtil.toJsonStr(loginRequest))
					.execute();

			JSONObject result = getData(response);
			String token = result.getStr("token");
			Long tokenExpire = result.getLong("tokenExpire");

			// 将 token 和过期时间存储到 Redis
			RedisUtils.set(TELEGRAM_TOKEN_KEY, token);
			RedisUtils.set(TELEGRAM_TOKEN_EXPIRE_KEY, tokenExpire * 1000L); // 转换为毫秒

			log.info("FeitApp登录成功，token过期时间: {}，已存储到Redis", tokenExpire);
			return token;
		} catch (Exception e) {
			log.error("FeitApp登录异常", e);
		}
		return null;
	}

	/**
	 * 获取有效token
	 */
	private String getValidToken() {
		// 从 Redis 获取 token 和过期时间
		String currentToken = RedisUtils.get(TELEGRAM_TOKEN_KEY);
		Long tokenExpireTime = RedisUtils.get(TELEGRAM_TOKEN_EXPIRE_KEY);

		// 检查token是否过期（提前5分钟刷新）
		if (StrUtil.isEmpty(currentToken) || tokenExpireTime == null ||
			System.currentTimeMillis() > (tokenExpireTime - 300000)) {
			currentToken = login();
		}
		return currentToken;
	}

	/**
	 * 获取response中的data
	 */
	private JSONObject getData(HttpResponse response) {
		if (response.getStatus() != 200) {
			log.error("请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
			throw new RuntimeException("请求失败，状态码: " + response.getStatus());
		}
		JSONObject result = JSONUtil.parseObj(response.body());
		Integer code = (Integer) result.get("code");
		if (code != 200) {
			log.error("请求失败，状态码: {}, 响应: {}", code, response.body());
			throw new RuntimeException("请求失败，状态码: " + code);
		}
		return result.getJSONObject("data");
	}

	private JSONArray getArray(HttpResponse response) {
		if (response.getStatus() != 200) {
			log.error("请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
			throw new RuntimeException("请求失败，状态码: " + response.getStatus());
		}
		JSONObject result = JSONUtil.parseObj(response.body());
		Integer code = (Integer) result.get("code");
		if (code != 200) {
			log.error("请求失败，状态码: {}, 响应: {}", code, response.body());
			throw new RuntimeException("请求失败，状态码: " + code);
		}
		return result.getJSONArray("data");
	}

	/**
	 * 获取response中的完整结果
	 */
	private Map<String, Object> getResult(HttpResponse response) {
		if (response.getStatus() != 200) {
			log.error("请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
			throw new RuntimeException("请求失败，状态码: " + response.getStatus());
		}
		Map<String, Object> result = JSONUtil.toBean(response.body(), Map.class);
		Integer code = (Integer) result.get("code");
		if (code != 200) {
			log.error("请求失败，状态码: {}, 响应: {}", code, response.body());
			throw new RuntimeException("请求失败，状态码: " + code);
		}
		return result;
	}

	/**
	 * 执行HTTP请求，自动处理401重新登录
	 */
	private HttpResponse executeRequest(String url, String method, Object requestBody) {
		String token = getValidToken();
		if (StrUtil.isEmpty(token)) {
			throw new RuntimeException("无法获取有效的访问令牌");
		}

		HttpRequest request;
		if ("GET".equalsIgnoreCase(method)) {
			request = HttpRequest.get(url);
		} else {
			request = HttpRequest.post(url);
			if (requestBody != null) {
				request.body(JSONUtil.toJsonStr(requestBody));
			}
		}

		HttpResponse response = request
				.header("Authorization", token)
				.header("Content-Type", "application/json")
				.execute();

		// 如果返回401，重新登录后再试一次
		if (response.getStatus() == 401) {
			log.warn("Token可能已过期，重新登录...");
			String newToken = login();
			if (StrUtil.isNotEmpty(newToken)) {
				response = request
						.header("Authorization", "Bearer " + newToken)
						.execute();
			}
		}

		return response;
	}

	@Override
	public R<List<TelegramAccount>> queryAccounts() {
		try {
			HttpResponse response = executeRequest(baseUrl + "/tgAccount/list", "GET", null);
			JSONArray result = getArray(response);
			List<TelegramAccount> accounts = result.stream().map(obj -> {
				JSONObject jo = (JSONObject) obj;
				TelegramAccount account = new TelegramAccount();
				account.setThirdPartyAccountId(jo.getLong("id"));
				account.setNickName(jo.getStr("firstName") + " " + jo.getStr("lastName"));
				account.setIsAvailable(!jo.getBool("isFrozen"));
				account.setUsername(jo.getStr("username"));
				account.setTgId(jo.getStr("tgId"));
				account.setPhone(jo.getStr("phone"));
				return account;
			}).collect(Collectors.toList());
			return R.ok(accounts);
		} catch (Exception e) {
			log.error("查询账户列表异常", e);
			return R.failed("查询账户列表异常: " + e.getMessage());
		}
	}

	@Override
	public R<List<TelegramGroup>> queryGroups(Integer pageIndex, Integer pageSize) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("pageIndex", pageIndex);
			requestBody.put("pageSize", pageSize);

			HttpResponse response = executeRequest(baseUrl + "/selfGroup/query", "POST", requestBody);
			JSONArray result = getArray(response);

			return R.ok(result.stream().map(obj -> {
				JSONObject jo = (JSONObject) obj;
				TelegramGroup group = new TelegramGroup();
				group.setThirdPartyAccountId(jo.getLong("id"));
				group.setGroupName(jo.getStr("title"));
				group.setUsername(jo.getStr("username"));
				return group;
			}).collect(Collectors.toList()));
		} catch (Exception e) {
			log.error("查询群组列表异常", e);
			return R.failed("查询群组列表异常: " + e.getMessage());
		}
	}

	@Override
	public R<List<TelegramGroup>> queryJoinedGroups(Long tgAccountId) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("tgAccountId", tgAccountId);
			requestBody.put("pageIndex", 1);
			requestBody.put("pageSize", 100);

			HttpResponse response = executeRequest(baseUrl + "/tgChat/query", "POST", requestBody);
			JSONArray result = getArray(response);

			return R.ok(result.stream().map(obj -> {
				JSONObject jo = (JSONObject) obj;
				TelegramGroup group = new TelegramGroup();
				group.setThirdPartyAccountId(jo.getLong("id"));
				group.setGroupName(jo.getStr("title"));
				group.setUsername(jo.getStr("username"));
				group.setSendScore(jo.getBool("canSendMessages") ? 2 : 0);
				return group;
			}).filter(group -> StringUtils.hasText(group.getUsername())).collect(Collectors.toList()));
		} catch (Exception e) {
			log.error("查询已加入群组异常", e);
			return R.failed("查询已加入群组异常: " + e.getMessage());
		}
	}

	@Override
	public R<CreateSendTaskResponse> createSendTask(CreateSendTaskRequest request) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("tgAccountId", request.getTgAccountId());
			requestBody.put("title", "SMM群发任务");
			requestBody.put("text", "[\"" + request.getText() + "\"]");
			requestBody.put("total", 1000);
			requestBody.put("messageType", 0);
			requestBody.put("timeUnit", request.getTimeUnit());
			requestBody.put("timeSpacingList", "[" + (request.getTimeSpacing() * 1000) + "," + (request.getTimeSpacing() * 1000) + "]");
			requestBody.put("executionTimeRange", "[\"00:00:00\",\"23:59:59\"]");
			requestBody.put("targetGroupList", request.getTargetGroupList());
			requestBody.put("autoRemoveNoPermissionChat", true);
			requestBody.put("isSafeMode", false);

			HttpResponse response = executeRequest(baseUrl + "/task/save", "POST", requestBody);
			Map<String, Object> result = getResult(response);
			CreateSendTaskResponse taskResponse = JSONUtil.toBean(JSONUtil.toJsonStr(result.get("data")), CreateSendTaskResponse.class);
			return R.ok(taskResponse);
		} catch (Exception e) {
			log.error("创建群发任务异常", e);
			return R.failed("创建群发任务异常: " + e.getMessage());
		}
	}

	@Override
	public R<SendRecordResponse> querySendRecord(Long taskId, Integer pageIndex, Integer pageSize) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("taskId", taskId);
			requestBody.put("pageIndex", pageIndex);
			requestBody.put("pageSize", pageSize);

			HttpResponse response = executeRequest(baseUrl + "/task/sendRecordQuery", "POST", requestBody);
			JSONArray result = getArray(response);
			// TODO
			return R.ok(new SendRecordResponse());
		} catch (Exception e) {
			log.error("查询发送记录异常", e);
			return R.failed("查询发送记录异常: " + e.getMessage());
		}
	}

	@Override
	public R<CreateJoinGroupTaskResponse> createJoinGroupTask(CreateJoinGroupTaskRequest request) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("tgAccountId", request.getTgAccountId());
			requestBody.put("id", request.getTaskId());
			requestBody.put(
					"targetGroupList",
					JSONUtil.toJsonStr(request.getTargetGroupList().stream().map(group -> {
						JSONObject jo = new JSONObject();
						jo.set("title", group.getGroupName());
						jo.set("username", group.getUsername());
						return jo;
					}).collect(Collectors.toList()))
					);
			requestBody.put("timeUnit", "m");
			requestBody.put("TimeSpacing", 3);
			requestBody.put("limit", request.getTargetGroupList().size());

			HttpResponse response = executeRequest(baseUrl + "/joinGroupTask/save", "POST", requestBody);
			Map<String, Object> result = getResult(response);
			CreateJoinGroupTaskResponse taskResponse = JSONUtil.toBean(JSONUtil.toJsonStr(result.get("data")), CreateJoinGroupTaskResponse.class);
			return R.ok(taskResponse);
		} catch (Exception e) {
			log.error("创建加群任务异常", e);
			return R.failed("创建加群任务异常: " + e.getMessage());
		}
	}

	@Override
	public R<Void> startJoinGroupTask(Long taskId) {
		try {
			HttpResponse response = executeRequest(baseUrl + "/joinGroupTask/start?id=" + taskId, "GET", null);
			getResult(response); // 验证响应，不需要返回数据
			return R.ok();
		} catch (Exception e) {
			log.error("启动加群任务异常", e);
			return R.failed("启动加群任务异常: " + e.getMessage());
		}
	}


	@Override
	public R<List<JoinTask>> queryJoinGroupTasks() {

		try {
			HttpResponse response = executeRequest(baseUrl + "/joinGroupTask/query", "GET", null);
			JSONArray result = getArray(response);
			return R.ok(result.stream().map(obj -> {
				JSONObject jo = (JSONObject) obj;
				JoinTask task = new JoinTask();
				task.setThirdPartyTaskId(jo.getLong("id"));
				task.setStatus(JoinTask.TaskStatusEnum.fromStatus(jo.getInt("status")));
				task.setTgAccountId(jo.getLong("tgAccountId"));
				return task;
			}).filter(joinTask -> joinTask.getThirdPartyTaskId() != null && !joinTask.getThirdPartyTaskId().equals(0L))
					.collect(Collectors.toList()));
		} catch (Exception e) {
			log.error("查询加群任务列表异常", e);
			return R.failed("查询加群任务列表异常: " + e.getMessage());
		}
	}

	@Override
	public R<List<GroupJoinResultDTO>> queryJoinGroupTaskDetails(Long taskId, Integer pageIndex, Integer pageSize) {
		try {
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("id", taskId);
			requestBody.put("pageIndex", pageIndex);
			requestBody.put("pageSize", pageSize);

			HttpResponse response = executeRequest(baseUrl + "/joinGroupTask/details", "POST", requestBody);
			JSONArray result = getArray(response);

			return R.ok(
					result.stream().map(obj -> {
						JSONObject jo = (JSONObject) obj;
						GroupJoinResultDTO groupJoinResultDTO = new GroupJoinResultDTO(
								jo.getStr("username"),
								JoinResultEnum.fromResult(jo.getInt("status")),
								jo.getStr("result")
						);
						return groupJoinResultDTO;
					}).collect(Collectors.toList())
				);
		} catch (Exception e) {
			log.error("查询加群任务详情异常", e);
			return R.failed("查询加群任务详情异常: " + e.getMessage());
		}
	}

}
