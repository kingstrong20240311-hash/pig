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

package com.pig4cloud.pig.order.match;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Matching Engine Configuration Properties
 *
 * @author lengleng
 * @date 2025/01/18
 */
@Data
@ConfigurationProperties(prefix = "matching-engine")
public class MatchingEngineProperties {

	/**
	 * Default asset symbol ID (used as currency ID in ExchangeCore) Default: 1
	 * (representing USDC)
	 */
	private int defaultAsset = 1;

	/**
	 * Asset ID to symbol name mapping Example: 1 -> USDC, 2 -> USDT, etc.
	 */
	private Map<Integer, String> assetSymbols = new HashMap<>();

	/**
	 * Whether to enable state recovery from order table on startup Default: true
	 */
	private boolean enableStateRecovery = true;

	/**
	 * Get asset symbol name by ID
	 * @param assetId asset ID
	 * @return symbol name, or "ASSET_{id}" if not found
	 */
	public String getAssetSymbol(int assetId) {
		return assetSymbols.getOrDefault(assetId, "ASSET_" + assetId);
	}

}
