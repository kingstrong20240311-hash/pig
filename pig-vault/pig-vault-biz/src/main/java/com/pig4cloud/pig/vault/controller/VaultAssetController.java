/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.vault.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.vault.api.dto.AssetResponse;
import com.pig4cloud.pig.vault.api.dto.CreateAssetRequest;
import com.pig4cloud.pig.vault.api.dto.UpdateAssetRequest;
import com.pig4cloud.pig.vault.service.VaultAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Vault Asset Controller
 *
 * @author luka
 * @date 2025-01-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/asset")
@Tag(description = "vault-asset", name = "Vault Asset Management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class VaultAssetController {

	private final VaultAssetService vaultAssetService;

	/**
	 * Create a new asset
	 * @param request create asset request
	 * @return asset response
	 */
	@PostMapping
	@Operation(summary = "Create Asset", description = "Create a new vault asset")
	public R<AssetResponse> createAsset(@Valid @RequestBody CreateAssetRequest request) {
		try {
			AssetResponse response = vaultAssetService.createAsset(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to create asset: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Update an existing asset
	 * @param request update asset request
	 * @return asset response
	 */
	@PutMapping
	@Operation(summary = "Update Asset", description = "Update an existing vault asset")
	public R<AssetResponse> updateAsset(@Valid @RequestBody UpdateAssetRequest request) {
		try {
			AssetResponse response = vaultAssetService.updateAsset(request);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to update asset: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Get asset by ID
	 * @param assetId asset ID
	 * @return asset response
	 */
	@GetMapping("/{assetId}")
	@Operation(summary = "Get Asset by ID", description = "Get vault asset by ID")
	public R<AssetResponse> getAssetById(@PathVariable("assetId") Long assetId) {
		try {
			AssetResponse response = vaultAssetService.getAssetById(assetId);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to get asset by ID: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Get asset by symbol
	 * @param symbol asset symbol
	 * @return asset response
	 */
	@GetMapping("/symbol/{symbol}")
	@Operation(summary = "Get Asset by Symbol", description = "Get vault asset by symbol")
	public R<AssetResponse> getAssetBySymbol(@PathVariable("symbol") String symbol) {
		try {
			AssetResponse response = vaultAssetService.getAssetBySymbol(symbol);
			return R.ok(response);
		}
		catch (Exception e) {
			log.error("Failed to get asset by symbol: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Get all assets
	 * @return list of asset responses
	 */
	@GetMapping("/list")
	@Operation(summary = "Get All Assets", description = "Get all vault assets")
	public R<List<AssetResponse>> getAllAssets() {
		try {
			List<AssetResponse> responses = vaultAssetService.getAllAssets();
			return R.ok(responses);
		}
		catch (Exception e) {
			log.error("Failed to get all assets: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Get all active assets
	 * @return list of active asset responses
	 */
	@GetMapping("/list/active")
	@Operation(summary = "Get Active Assets", description = "Get all active vault assets")
	public R<List<AssetResponse>> getActiveAssets() {
		try {
			List<AssetResponse> responses = vaultAssetService.getActiveAssets();
			return R.ok(responses);
		}
		catch (Exception e) {
			log.error("Failed to get active assets: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Deactivate an asset
	 * @param assetId asset ID
	 * @return success response
	 */
	@PostMapping("/{assetId}/deactivate")
	@Operation(summary = "Deactivate Asset", description = "Deactivate a vault asset")
	public R<Void> deactivateAsset(@PathVariable("assetId") Long assetId) {
		try {
			vaultAssetService.deactivateAsset(assetId);
			return R.ok();
		}
		catch (Exception e) {
			log.error("Failed to deactivate asset: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

	/**
	 * Activate an asset
	 * @param assetId asset ID
	 * @return success response
	 */
	@PostMapping("/{assetId}/activate")
	@Operation(summary = "Activate Asset", description = "Activate a vault asset")
	public R<Void> activateAsset(@PathVariable("assetId") Long assetId) {
		try {
			vaultAssetService.activateAsset(assetId);
			return R.ok();
		}
		catch (Exception e) {
			log.error("Failed to activate asset: {}", e.getMessage(), e);
			return R.failed(e.getMessage());
		}
	}

}
