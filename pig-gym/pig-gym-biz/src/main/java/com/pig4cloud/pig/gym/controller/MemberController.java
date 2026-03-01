/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pig.gym.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.gym.api.entity.Member;
import com.pig4cloud.pig.gym.service.MemberService;
import com.pig4cloud.plugin.excel.annotation.ResponseExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 会员控制器
 *
 * @author claude
 * @date 2026-02-25
 */
@RestController
@AllArgsConstructor
@RequestMapping("${gym.api-prefix:}/member")
@Tag(description = "member", name = "会员管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class MemberController {

	private final MemberService memberService;

	/**
	 * 分页查询会员
	 * @param page 分页对象
	 * @param member 查询条件
	 * @return 分页查询结果
	 */
	@GetMapping("/page")
	@Operation(description = "分页查询", summary = "分页查询会员")
	public R getPage(@ParameterObject Page page, @ParameterObject Member member) {
		LambdaQueryWrapper<Member> wrapper = Wrappers.<Member>lambdaQuery()
			.like(StringUtils.hasText(member.getName()), Member::getName, member.getName())
			.like(StringUtils.hasText(member.getMobile()), Member::getMobile, member.getMobile())
			.eq(member.getDeptId() != null, Member::getDeptId, member.getDeptId())
			.eq(member.getCoachId() != null, Member::getCoachId, member.getCoachId())
			.eq(member.getEnabled() != null, Member::getEnabled, member.getEnabled())
			.orderByDesc(Member::getCreateTime);

		return R.ok(memberService.page(page, wrapper));
	}

	/**
	 * 通过id查询会员
	 * @param id 会员id
	 * @return 包含查询结果的响应对象
	 */
	@Operation(description = "通过id查询会员", summary = "通过id查询会员")
	@GetMapping("/details/{id}")
	public R getById(@PathVariable("id") Long id) {
		return R.ok(memberService.getById(id));
	}

	/**
	 * 新增会员
	 * @param member 会员对象
	 * @return 操作结果
	 */
	@PostMapping
	@SysLog("新增会员")
	@Operation(description = "新增会员", summary = "新增会员")
	@HasPermission("gym_member_add")
	public R save(@RequestBody Member member) {
		return R.ok(memberService.save(member));
	}

	/**
	 * 修改会员
	 * @param member 会员对象
	 * @return 操作结果
	 */
	@PutMapping
	@SysLog("修改会员")
	@HasPermission("gym_member_edit")
	@Operation(description = "修改会员", summary = "修改会员")
	public R update(@RequestBody Member member) {
		return R.ok(memberService.updateById(member));
	}

	/**
	 * 通过id数组删除会员
	 * @param ids 要删除的会员id数组
	 * @return 操作结果
	 */
	@DeleteMapping
	@SysLog("删除会员")
	@HasPermission("gym_member_del")
	@Operation(description = "删除会员", summary = "删除会员")
	public R removeById(@RequestBody Long[] ids) {
		return R.ok(memberService.removeBatchByIds(List.of(ids)));
	}

	/**
	 * 导出excel表格
	 * @param member 查询条件
	 * @return 会员列表
	 */
	@ResponseExcel
	@GetMapping("/export")
	@HasPermission("gym_member_export")
	@Operation(description = "导出会员", summary = "导出会员")
	public List<Member> export(@ParameterObject Member member) {
		LambdaQueryWrapper<Member> wrapper = Wrappers.<Member>lambdaQuery()
			.like(StringUtils.hasText(member.getName()), Member::getName, member.getName())
			.like(StringUtils.hasText(member.getMobile()), Member::getMobile, member.getMobile())
			.eq(member.getDeptId() != null, Member::getDeptId, member.getDeptId())
			.eq(member.getCoachId() != null, Member::getCoachId, member.getCoachId())
			.eq(member.getEnabled() != null, Member::getEnabled, member.getEnabled());

		return memberService.list(wrapper);
	}

}
