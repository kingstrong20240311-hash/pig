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

package com.pig4cloud.pig.vault.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.vault.api.dto.AssetResponse;
import com.pig4cloud.pig.vault.api.dto.CreateAssetRequest;
import com.pig4cloud.pig.vault.api.dto.UpdateAssetRequest;
import com.pig4cloud.pig.vault.api.entity.VaultAsset;
import com.pig4cloud.pig.vault.mapper.VaultAssetMapper;
import com.pig4cloud.pig.vault.service.VaultAssetService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Vault Asset Service Implementation
 *
 * @author luka
 * @date 2025-01-24
 */
@Service
@RequiredArgsConstructor
public class VaultAssetServiceImpl implements VaultAssetService {

	private static final Logger log = LoggerFactory.getLogger(VaultAssetServiceImpl.class);

	private final VaultAssetMapper vaultAssetMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AssetResponse createAsset(CreateAssetRequest request) {
		// Check if asset with same symbol already exists
		VaultAsset existingAsset = vaultAssetMapper
			.selectOne(Wrappers.<VaultAsset>lambdaQuery().eq(VaultAsset::getSymbol, request.getSymbol()));

		if (existingAsset != null) {
			throw new IllegalArgumentException("Asset with symbol " + request.getSymbol() + " already exists");
		}

		// Create new asset
		VaultAsset asset = new VaultAsset();
		asset.setSymbol(request.getSymbol().toUpperCase());
		asset.setCurrencyId(request.getCurrencyId());
		asset.setDecimals(request.getDecimals());
		asset.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

		vaultAssetMapper.insert(asset);

		log.info("Created new asset: symbol={}, decimals={}, isActive={}", asset.getSymbol(), asset.getDecimals(),
				asset.getIsActive());

		return toAssetResponse(asset);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AssetResponse updateAsset(UpdateAssetRequest request) {
		VaultAsset asset = vaultAssetMapper.selectById(request.getAssetId());

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found with ID: " + request.getAssetId());
		}

		// Update fields if provided
		if (request.getDecimals() != null) {
			asset.setDecimals(request.getDecimals());
		}
		if (request.getIsActive() != null) {
			asset.setIsActive(request.getIsActive());
		}

		vaultAssetMapper.updateById(asset);

		log.info("Updated asset: assetId={}, symbol={}, decimals={}, isActive={}", asset.getAssetId(),
				asset.getSymbol(), asset.getDecimals(), asset.getIsActive());

		return toAssetResponse(asset);
	}

	@Override
	public AssetResponse getAssetById(Long assetId) {
		VaultAsset asset = vaultAssetMapper.selectById(assetId);

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found with ID: " + assetId);
		}

		return toAssetResponse(asset);
	}

	@Override
	public AssetResponse getAssetBySymbol(String symbol) {
		VaultAsset asset = vaultAssetMapper
			.selectOne(Wrappers.<VaultAsset>lambdaQuery().eq(VaultAsset::getSymbol, symbol.toUpperCase()));

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found with symbol: " + symbol);
		}

		return toAssetResponse(asset);
	}

	@Override
	public List<AssetResponse> getAllAssets() {
		List<VaultAsset> assets = vaultAssetMapper
			.selectList(Wrappers.<VaultAsset>lambdaQuery().orderByDesc(VaultAsset::getCreateTime));

		return assets.stream().map(this::toAssetResponse).collect(Collectors.toList());
	}

	@Override
	public List<AssetResponse> getActiveAssets() {
		List<VaultAsset> assets = vaultAssetMapper.selectList(Wrappers.<VaultAsset>lambdaQuery()
			.eq(VaultAsset::getIsActive, true)
			.orderByDesc(VaultAsset::getCreateTime));

		return assets.stream().map(this::toAssetResponse).collect(Collectors.toList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deactivateAsset(Long assetId) {
		VaultAsset asset = vaultAssetMapper.selectById(assetId);

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found with ID: " + assetId);
		}

		asset.setIsActive(false);
		vaultAssetMapper.updateById(asset);

		log.info("Deactivated asset: assetId={}, symbol={}", asset.getAssetId(), asset.getSymbol());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void activateAsset(Long assetId) {
		VaultAsset asset = vaultAssetMapper.selectById(assetId);

		if (asset == null) {
			throw new IllegalArgumentException("Asset not found with ID: " + assetId);
		}

		asset.setIsActive(true);
		vaultAssetMapper.updateById(asset);

		log.info("Activated asset: assetId={}, symbol={}", asset.getAssetId(), asset.getSymbol());
	}

	/**
	 * Convert VaultAsset entity to AssetResponse DTO
	 */
	private AssetResponse toAssetResponse(VaultAsset asset) {
		AssetResponse response = new AssetResponse();
		response.setAssetId(asset.getAssetId() != null ? String.valueOf(asset.getAssetId()) : null);
		response.setSymbol(asset.getSymbol());
		response.setCurrencyId(asset.getCurrencyId());
		response.setDecimals(asset.getDecimals());
		response.setIsActive(asset.getIsActive());
		response.setCreateTime(asset.getCreateTime() != null ? asset.getCreateTime().toEpochMilli() : null);
		return response;
	}

}
