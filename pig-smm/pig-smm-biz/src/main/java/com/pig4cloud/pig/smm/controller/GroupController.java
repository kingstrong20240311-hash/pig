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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.smm.api.entity.SmmGroup;
import com.pig4cloud.pig.smm.service.GroupService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 群组管理控制器
 *
 * @author lengleng
 * @date 2025-09-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/group")
@Tag(description = "group", name = "群组管理模块")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class GroupController {

	private final GroupService groupService;

	/**
	 * 分页查询群组
	 * @param page 参数集
	 * @param smmGroup 查询参数列表
	 * @return 群组集合
	 */
	@GetMapping("/page")
	@Operation(summary = "分页查询群组", description = "分页查询群组")
	public R getGroupPage(@ParameterObject Page page, @ParameterObject SmmGroup smmGroup) {
		return R.ok(groupService.page(page, Wrappers.query(smmGroup)));
	}

	/**
	 * 通过ID查询群组信息
	 * @param id 群组ID
	 * @return 包含群组信息的响应对象
	 */
	@GetMapping("/details/{id}")
	@Operation(summary = "查询群组详情", description = "根据ID查询群组详情")
	public R getGroup(@PathVariable Long id) {
		return R.ok(groupService.getById(id));
	}

	/**
	 * 添加群组
	 * @param smmGroup 群组信息
	 * @return 操作结果，成功返回success，失败返回false
	 */
	@SysLog("添加群组")
	@PostMapping
	@HasPermission("smm_group_add")
	@Operation(summary = "添加群组", description = "添加群组")
	public R saveGroup(@Valid @RequestBody SmmGroup smmGroup) {
		return R.ok(groupService.save(smmGroup));
	}

	/**
	 * 更新群组信息
	 * @param smmGroup 群组信息对象
	 * @return 包含操作结果的R对象
	 */
	@SysLog("更新群组信息")
	@PutMapping
	@HasPermission("smm_group_edit")
	@Operation(summary = "更新群组", description = "更新群组信息")
	public R updateGroup(@Valid @RequestBody SmmGroup smmGroup) {
		return R.ok(groupService.updateById(smmGroup));
	}

	/**
	 * 删除群组信息
	 * @param ids 群组ID数组
	 * @return 操作结果
	 */
	@SysLog("删除群组信息")
	@DeleteMapping
	@HasPermission("smm_group_del")
	@Operation(summary = "删除群组", description = "根据ID删除群组")
	public R deleteGroup(@RequestBody Long[] ids) {
		return R.ok(groupService.removeByIds(java.util.Arrays.asList(ids)));
	}

}
