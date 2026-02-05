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

package com.pig4cloud.pig.order.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.order.api.dto.CreateMarketRequest;
import com.pig4cloud.pig.order.api.dto.MarketDTO;
import com.pig4cloud.pig.order.api.dto.UpdateMarketStatusRequest;
import com.pig4cloud.pig.order.api.entity.Market;
import com.pig4cloud.pig.order.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

/**
 * Market Controller
 *
 * @author lengleng
 * @date 2025/01/23
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/market")
@Tag(description = "market", name = "市场管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class MarketController {

	private final MarketService marketService;

	/**
	 * Create a new market
	 * @param request create market request
	 * @return market ID
	 */
	@PostMapping
	@PreAuthorize("@pms.hasPermission('market_create')")
	@Operation(summary = "创建市场", description = "创建新的预测市场")
	public R<String> createMarket(@Valid @RequestBody CreateMarketRequest request) {
		Long marketId = marketService.createMarket(request);
		return R.ok(String.valueOf(marketId));
	}

	/**
	 * Get market by ID
	 * @param marketId market ID
	 * @return market details
	 */
	@GetMapping("/{marketId}")
	@Operation(summary = "查询市场", description = "根据市场ID查询市场详情")
	public R<MarketDTO> getMarket(@PathVariable("marketId") Long marketId) {
		Market market = marketService.getMarket(marketId);
		if (market == null) {
			return R.failed("Market not found");
		}
		return R.ok(toMarketDTO(market));
	}

	/**
	 * Get active markets
	 * @param now optional timestamp, defaults to server current time
	 * @return list of active markets
	 */
	@GetMapping("/active")
	@Operation(summary = "查询有效市场列表", description = "查询当前有效的市场列表")
	public R<List<MarketDTO>> getActiveMarkets(@RequestParam(value = "now", required = false) Instant now) {
		if (now == null) {
			now = Instant.now();
		}
		List<Market> markets = marketService.listActiveMarkets(now);
		List<MarketDTO> marketDTOs = markets.stream().map(this::toMarketDTO).toList();
		return R.ok(marketDTOs);
	}

	/**
	 * Update market status
	 * @param marketId market ID
	 * @param request update market status request
	 * @return success
	 */
	@PatchMapping("/{marketId}/status")
	@PreAuthorize("@pms.hasPermission('market_update')")
	@Operation(summary = "更新市场状态", description = "上下架/状态切换")
	public R<Void> updateMarketStatus(@PathVariable("marketId") Long marketId,
			@Valid @RequestBody UpdateMarketStatusRequest request) {
		marketService.updateMarketStatus(marketId, request.getStatus());
		return R.ok();
	}

	/**
	 * Delete market (soft delete)
	 * @param marketId market ID
	 * @return success
	 */
	@DeleteMapping("/{marketId}")
	@PreAuthorize("@pms.hasPermission('market_delete')")
	@Operation(summary = "删除市场", description = "逻辑删除市场（更新 delFlag=1）")
	public R<Void> deleteMarket(@PathVariable("marketId") Long marketId) {
		marketService.deleteMarket(marketId);
		return R.ok();
	}

	/**
	 * Convert Market entity to MarketDTO
	 * @param market Market entity
	 * @return MarketDTO
	 */
	private MarketDTO toMarketDTO(Market market) {
		MarketDTO dto = new MarketDTO();
		dto.setMarketId(market.getMarketId() != null ? String.valueOf(market.getMarketId()) : null);
		dto.setName(market.getName());
		dto.setSymbolIdYes(market.getSymbolIdYes());
		dto.setSymbolIdNo(market.getSymbolIdNo());
		dto.setStatus(market.getStatus());
		dto.setExpireAt(market.getExpireAt() != null ? market.getExpireAt().toEpochMilli() : null);
		dto.setCreateTime(market.getCreateTime() != null ? market.getCreateTime().toEpochMilli() : null);
		dto.setUpdateTime(market.getUpdateTime() != null ? market.getUpdateTime().toEpochMilli() : null);
		dto.setDelFlag(market.getDelFlag());
		return dto;
	}

}
