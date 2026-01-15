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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.vault.PigVaultApplication;
import com.pig4cloud.pig.vault.api.dto.CreateFreezeRequest;
import com.pig4cloud.pig.vault.api.dto.FreezeLookupRequest;
import com.pig4cloud.pig.vault.api.entity.Balance;
import com.pig4cloud.pig.vault.api.entity.Freeze;
import com.pig4cloud.pig.vault.api.entity.LedgerEntry;
import com.pig4cloud.pig.vault.api.enums.FreezeStatus;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.mapper.BalanceMapper;
import com.pig4cloud.pig.vault.mapper.FreezeMapper;
import com.pig4cloud.pig.vault.mapper.LedgerEntryMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vault Controller Integration Test
 *
 * @author luka
 * @date 2025-01-15
 */
@SpringBootTest(classes = PigVaultApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
class VaultControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BalanceMapper balanceMapper;

	@Autowired
	private FreezeMapper freezeMapper;

	@Autowired
	private LedgerEntryMapper ledgerEntryMapper;

	@Autowired
	private DataSource dataSource;

	private static final Long ACCOUNT_ID = 1001L;

	private static final Long ASSET_ID = 1L;

	private static final BigDecimal INITIAL_AVAILABLE = new BigDecimal("100.000000");

	@BeforeEach
	void setUp() throws Exception {
		// Execute test data initialization script
		try (Connection conn = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(conn, new ClassPathResource("test-data.sql"));
		}
	}

	/**
	 * Test Case 1: Create freeze successfully
	 */
	@Test
	@Order(1)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("1. Create freeze successfully")
	void testCreateFreezeSuccess() throws Exception {
		CreateFreezeRequest request = new CreateFreezeRequest();
		request.setAccountId(ACCOUNT_ID);
		request.setAssetId(ASSET_ID);
		request.setAmount(new BigDecimal("10.000000"));
		request.setRefType(RefType.ORDER);
		request.setRefId("ORD-001");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.status").value("HELD"))
			.andExpect(jsonPath("$.data.amount").value(10.0));

		// Verify balance changes
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("90.000000"));
		assertThat(balance.getFrozen()).isEqualByComparingTo(new BigDecimal("10.000000"));

		// Verify freeze record created
		Freeze freeze = freezeMapper
			.selectOne(Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.ORDER).eq(Freeze::getRefId, "ORD-001"));
		assertThat(freeze).isNotNull();
		assertThat(freeze.getStatus()).isEqualTo(FreezeStatus.HELD);

		// Verify ledger entry created
		long ledgerCount = ledgerEntryMapper.selectCount(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getRefType, RefType.ORDER)
			.eq(LedgerEntry::getRefId, "ORD-001"));
		assertThat(ledgerCount).isGreaterThan(0);
	}

	/**
	 * Test Case 2: Create freeze idempotency (same refType+refId)
	 */
	@Test
	@Order(2)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("2. Create freeze idempotency")
	void testCreateFreezeIdempotency() throws Exception {
		// First request
		CreateFreezeRequest request = new CreateFreezeRequest();
		request.setAccountId(ACCOUNT_ID);
		request.setAssetId(ASSET_ID);
		request.setAmount(new BigDecimal("15.000000"));
		request.setRefType(RefType.ORDER);
		request.setRefId("ORD-002");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.status").value("HELD"))
			.andExpect(jsonPath("$.data.amount").value(15.0));

		// Get freeze ID from database
		Freeze freeze1 = freezeMapper
			.selectOne(Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.ORDER).eq(Freeze::getRefId, "ORD-002"));
		Long freezeId1 = freeze1.getFreezeId();

		// Second request with same refType+refId should return same freeze
		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.freezeId").value(freezeId1))
			.andExpect(jsonPath("$.data.status").value("HELD"))
			.andExpect(jsonPath("$.data.amount").value(15.0));

		// Verify balance changed only once
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("85.000000"));
		assertThat(balance.getFrozen()).isEqualByComparingTo(new BigDecimal("15.000000"));
	}

	/**
	 * Test Case 3: Create freeze with insufficient balance
	 */
	@Test
	@Order(3)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("3. Create freeze with insufficient balance")
	void testCreateFreezeInsufficientBalance() throws Exception {
		CreateFreezeRequest request = new CreateFreezeRequest();
		request.setAccountId(ACCOUNT_ID);
		request.setAssetId(ASSET_ID);
		request.setAmount(new BigDecimal("200.000000")); // More than available
		request.setRefType(RefType.ORDER);
		request.setRefId("ORD-003");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1));

		// Verify balance unchanged
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(INITIAL_AVAILABLE);
		assertThat(balance.getFrozen()).isEqualByComparingTo(BigDecimal.ZERO);

		// Verify no freeze created
		Freeze freeze = freezeMapper
			.selectOne(Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.ORDER).eq(Freeze::getRefId, "ORD-003"));
		assertThat(freeze).isNull();
	}

	/**
	 * Test Case 4: Create freeze with validation errors
	 */
	@Test
	@Order(4)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("4. Create freeze with validation errors")
	void testCreateFreezeValidationErrors() throws Exception {
		// Missing accountId
		CreateFreezeRequest request1 = new CreateFreezeRequest();
		request1.setAssetId(ASSET_ID);
		request1.setAmount(new BigDecimal("10.000000"));
		request1.setRefType(RefType.ORDER);
		request1.setRefId("ORD-004");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request1)))
			.andDo(print())
			.andExpect(status().is4xxClientError());

		// Negative amount
		CreateFreezeRequest request2 = new CreateFreezeRequest();
		request2.setAccountId(ACCOUNT_ID);
		request2.setAssetId(ASSET_ID);
		request2.setAmount(new BigDecimal("-10.000000"));
		request2.setRefType(RefType.ORDER);
		request2.setRefId("ORD-005");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request2)))
			.andDo(print())
			.andExpect(status().is4xxClientError());
	}

	/**
	 * Test Case 5: Release freeze successfully
	 */
	@Test
	@Order(5)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("5. Release freeze successfully")
	@Transactional
	void testReleaseFreezeSuccess() throws Exception {
		// First create a freeze
		CreateFreezeRequest createRequest = new CreateFreezeRequest();
		createRequest.setAccountId(ACCOUNT_ID);
		createRequest.setAssetId(ASSET_ID);
		createRequest.setAmount(new BigDecimal("20.000000"));
		createRequest.setRefType(RefType.ORDER);
		createRequest.setRefId("ORD-006");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isOk());

		// Then release it
		FreezeLookupRequest releaseRequest = new FreezeLookupRequest();
		releaseRequest.setRefType(RefType.ORDER);
		releaseRequest.setRefId("ORD-006");

		mockMvc.perform(post("/vault/freeze/release").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(releaseRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.status").value("RELEASED"));

		// Verify balance restored
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(INITIAL_AVAILABLE);
		assertThat(balance.getFrozen()).isEqualByComparingTo(BigDecimal.ZERO);

		// Verify freeze status updated
		Freeze freeze = freezeMapper
			.selectOne(Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.ORDER).eq(Freeze::getRefId, "ORD-006"));
		assertThat(freeze.getStatus()).isEqualTo(FreezeStatus.RELEASED);
	}

	/**
	 * Test Case 6: Release freeze idempotency
	 */
	@Test
	@Order(6)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("6. Release freeze idempotency")
	@Transactional
	void testReleaseFreezeIdempotency() throws Exception {
		// Create a freeze
		CreateFreezeRequest createRequest = new CreateFreezeRequest();
		createRequest.setAccountId(ACCOUNT_ID);
		createRequest.setAssetId(ASSET_ID);
		createRequest.setAmount(new BigDecimal("25.000000"));
		createRequest.setRefType(RefType.ORDER);
		createRequest.setRefId("ORD-007");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isOk());

		// Release first time
		FreezeLookupRequest releaseRequest = new FreezeLookupRequest();
		releaseRequest.setRefType(RefType.ORDER);
		releaseRequest.setRefId("ORD-007");

		mockMvc.perform(post("/vault/freeze/release").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(releaseRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RELEASED"));

		long ledgerCountAfterFirstRelease = ledgerEntryMapper.selectCount(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getRefType, RefType.ORDER)
			.eq(LedgerEntry::getRefId, "ORD-007"));

		// Release second time (idempotent)
		mockMvc.perform(post("/vault/freeze/release").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(releaseRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RELEASED"));

		// Verify no new ledger entry
		long ledgerCountAfterSecondRelease = ledgerEntryMapper.selectCount(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getRefType, RefType.ORDER)
			.eq(LedgerEntry::getRefId, "ORD-007"));
		assertThat(ledgerCountAfterSecondRelease).isEqualTo(ledgerCountAfterFirstRelease);
	}

	/**
	 * Test Case 7: Claim freeze successfully
	 */
	@Test
	@Order(7)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("7. Claim freeze successfully")
	@Transactional
	void testClaimFreezeSuccess() throws Exception {
		// Create a freeze
		CreateFreezeRequest createRequest = new CreateFreezeRequest();
		createRequest.setAccountId(ACCOUNT_ID);
		createRequest.setAssetId(ASSET_ID);
		createRequest.setAmount(new BigDecimal("30.000000"));
		createRequest.setRefType(RefType.SETTLEMENT);
		createRequest.setRefId("SETTLE-001");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isOk());

		// Claim it
		FreezeLookupRequest claimRequest = new FreezeLookupRequest();
		claimRequest.setRefType(RefType.SETTLEMENT);
		claimRequest.setRefId("SETTLE-001");

		mockMvc.perform(post("/vault/freeze/claim").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(claimRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.status").value("CLAIMED"))
			.andExpect(jsonPath("$.data.claimTime").isNotEmpty());

		// Verify freeze status and claim time
		Freeze freeze = freezeMapper.selectOne(
				Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.SETTLEMENT).eq(Freeze::getRefId, "SETTLE-001"));
		assertThat(freeze.getStatus()).isEqualTo(FreezeStatus.CLAIMED);
		assertThat(freeze.getClaimTime()).isNotNull();

		// Verify balance unchanged (frozen stays frozen)
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getFrozen()).isEqualByComparingTo(new BigDecimal("30.000000"));
	}

	/**
	 * Test Case 8: Claim freeze with invalid status
	 */
	@Test
	@Order(8)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("8. Claim freeze with invalid status")
	@Transactional
	void testClaimFreezeInvalidStatus() throws Exception {
		// Create and release a freeze
		CreateFreezeRequest createRequest = new CreateFreezeRequest();
		createRequest.setAccountId(ACCOUNT_ID);
		createRequest.setAssetId(ASSET_ID);
		createRequest.setAmount(new BigDecimal("35.000000"));
		createRequest.setRefType(RefType.SETTLEMENT);
		createRequest.setRefId("SETTLE-002");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isOk());

		FreezeLookupRequest releaseRequest = new FreezeLookupRequest();
		releaseRequest.setRefType(RefType.SETTLEMENT);
		releaseRequest.setRefId("SETTLE-002");

		mockMvc.perform(post("/vault/freeze/release").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(releaseRequest)))
			.andExpect(status().isOk());

		// Try to claim already released freeze
		FreezeLookupRequest claimRequest = new FreezeLookupRequest();
		claimRequest.setRefType(RefType.SETTLEMENT);
		claimRequest.setRefId("SETTLE-002");

		mockMvc.perform(post("/vault/freeze/claim").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(claimRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1));

		// Verify status unchanged
		Freeze freeze = freezeMapper.selectOne(
				Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.SETTLEMENT).eq(Freeze::getRefId, "SETTLE-002"));
		assertThat(freeze.getStatus()).isEqualTo(FreezeStatus.RELEASED);
	}

	/**
	 * Test Case 9: Consume freeze successfully from HELD status
	 */
	@Test
	@Order(9)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("9. Consume freeze successfully from HELD")
	@Transactional
	void testConsumeFreezeFromHeld() throws Exception {
		// Create a freeze
		CreateFreezeRequest createRequest = new CreateFreezeRequest();
		createRequest.setAccountId(ACCOUNT_ID);
		createRequest.setAssetId(ASSET_ID);
		createRequest.setAmount(new BigDecimal("40.000000"));
		createRequest.setRefType(RefType.ORDER);
		createRequest.setRefId("ORD-008");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isOk());

		// Consume it
		FreezeLookupRequest consumeRequest = new FreezeLookupRequest();
		consumeRequest.setRefType(RefType.ORDER);
		consumeRequest.setRefId("ORD-008");

		mockMvc.perform(post("/vault/freeze/consume").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(consumeRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.status").value("CONSUMED"));

		// Verify balance: frozen decreased, available unchanged
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("60.000000"));
		assertThat(balance.getFrozen()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	/**
	 * Test Case 10: Consume freeze successfully from CLAIMED status
	 */
	@Test
	@Order(10)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("10. Consume freeze successfully from CLAIMED")
	@Transactional
	void testConsumeFreezeFromClaimed() throws Exception {
		// Create a freeze
		CreateFreezeRequest createRequest = new CreateFreezeRequest();
		createRequest.setAccountId(ACCOUNT_ID);
		createRequest.setAssetId(ASSET_ID);
		createRequest.setAmount(new BigDecimal("50.000000"));
		createRequest.setRefType(RefType.SETTLEMENT);
		createRequest.setRefId("SETTLE-003");

		mockMvc.perform(post("/vault/freeze/create").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isOk());

		// Claim it first
		FreezeLookupRequest claimRequest = new FreezeLookupRequest();
		claimRequest.setRefType(RefType.SETTLEMENT);
		claimRequest.setRefId("SETTLE-003");

		mockMvc.perform(post("/vault/freeze/claim").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(claimRequest)))
			.andExpect(status().isOk());

		// Then consume it
		FreezeLookupRequest consumeRequest = new FreezeLookupRequest();
		consumeRequest.setRefType(RefType.SETTLEMENT);
		consumeRequest.setRefId("SETTLE-003");

		mockMvc.perform(post("/vault/freeze/consume").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(consumeRequest)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.status").value("CONSUMED"));

		// Verify freeze status
		Freeze freeze = freezeMapper.selectOne(
				Wrappers.<Freeze>lambdaQuery().eq(Freeze::getRefType, RefType.SETTLEMENT).eq(Freeze::getRefId, "SETTLE-003"));
		assertThat(freeze.getStatus()).isEqualTo(FreezeStatus.CONSUMED);
	}

	/**
	 * Test: Get balance successfully
	 */
	@Test
	@Order(11)
	@WithMockUser(username = "test", roles = "USER")
	@DisplayName("Get balance successfully")
	void testGetBalanceSuccess() throws Exception {
		mockMvc.perform(get("/vault/balance").param("accountId", ACCOUNT_ID.toString())
			.param("assetId", ASSET_ID.toString()))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
			.andExpect(jsonPath("$.data.assetId").value(ASSET_ID))
			.andExpect(jsonPath("$.data.available").exists())
			.andExpect(jsonPath("$.data.frozen").exists());
	}

}
