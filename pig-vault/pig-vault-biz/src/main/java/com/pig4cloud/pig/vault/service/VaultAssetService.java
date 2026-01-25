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

package com.pig4cloud.pig.vault.service;

import com.pig4cloud.pig.vault.api.dto.AssetResponse;
import com.pig4cloud.pig.vault.api.dto.CreateAssetRequest;
import com.pig4cloud.pig.vault.api.dto.UpdateAssetRequest;

import java.util.List;

/**
 * Vault Asset Service
 *
 * @author luka
 * @date 2025-01-24
 */
public interface VaultAssetService {

	/**
	 * Create a new asset
	 * @param request create asset request
	 * @return asset response
	 */
	AssetResponse createAsset(CreateAssetRequest request);

	/**
	 * Update an existing asset
	 * @param request update asset request
	 * @return asset response
	 */
	AssetResponse updateAsset(UpdateAssetRequest request);

	/**
	 * Get asset by ID
	 * @param assetId asset ID
	 * @return asset response
	 */
	AssetResponse getAssetById(Long assetId);

	/**
	 * Get asset by symbol
	 * @param symbol asset symbol
	 * @return asset response
	 */
	AssetResponse getAssetBySymbol(String symbol);

	/**
	 * Get all assets
	 * @return list of asset responses
	 */
	List<AssetResponse> getAllAssets();

	/**
	 * Get all active assets
	 * @return list of active asset responses
	 */
	List<AssetResponse> getActiveAssets();

	/**
	 * Deactivate an asset
	 * @param assetId asset ID
	 */
	void deactivateAsset(Long assetId);

	/**
	 * Activate an asset
	 * @param assetId asset ID
	 */
	void activateAsset(Long assetId);

}
