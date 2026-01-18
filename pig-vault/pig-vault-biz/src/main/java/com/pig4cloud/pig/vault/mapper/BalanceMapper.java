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

package com.pig4cloud.pig.vault.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.vault.api.entity.Balance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * Balance Mapper
 *
 * @author luka
 * @date 2025-01-14
 */
@Mapper
public interface BalanceMapper extends BaseMapper<Balance> {

	/**
	 * Update balance for freeze operation (available -> frozen) Uses optimistic locking
	 * with version
	 * @param accountId account ID
	 * @param assetId asset ID
	 * @param amount amount to freeze
	 * @param version current version
	 * @return number of rows affected
	 */
	@Update("UPDATE vault_balance SET " + "available = available - #{amount}, " + "frozen = frozen + #{amount}, "
			+ "version = version + 1 " + "WHERE account_id = #{accountId} " + "AND asset_id = #{assetId} "
			+ "AND version = #{version} " + "AND available >= #{amount}")
	int freezeBalance(@Param("accountId") Long accountId, @Param("assetId") Long assetId,
			@Param("amount") BigDecimal amount, @Param("version") Long version);

	/**
	 * Update balance for unfreeze operation (frozen -> available)
	 * @param accountId account ID
	 * @param assetId asset ID
	 * @param amount amount to unfreeze
	 * @param version current version
	 * @return number of rows affected
	 */
	@Update("UPDATE vault_balance SET " + "available = available + #{amount}, " + "frozen = frozen - #{amount}, "
			+ "version = version + 1 " + "WHERE account_id = #{accountId} " + "AND asset_id = #{assetId} "
			+ "AND version = #{version} " + "AND frozen >= #{amount}")
	int unfreezeBalance(@Param("accountId") Long accountId, @Param("assetId") Long assetId,
			@Param("amount") BigDecimal amount, @Param("version") Long version);

	/**
	 * Update balance for consume operation (frozen -> spent)
	 * @param accountId account ID
	 * @param assetId asset ID
	 * @param amount amount to consume
	 * @param version current version
	 * @return number of rows affected
	 */
	@Update("UPDATE vault_balance SET " + "frozen = frozen - #{amount}, " + "version = version + 1 "
			+ "WHERE account_id = #{accountId} " + "AND asset_id = #{assetId} " + "AND version = #{version} "
			+ "AND frozen >= #{amount}")
	int consumeFreeze(@Param("accountId") Long accountId, @Param("assetId") Long assetId,
			@Param("amount") BigDecimal amount, @Param("version") Long version);

}
