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
import com.pig4cloud.pig.vault.api.dto.DepositRequest;
import com.pig4cloud.pig.vault.api.entity.Balance;
import com.pig4cloud.pig.vault.api.entity.LedgerEntry;
import com.pig4cloud.pig.vault.api.entity.VaultAccount;
import com.pig4cloud.pig.vault.api.entity.VaultAsset;
import com.pig4cloud.pig.vault.api.enums.AccountStatus;
import com.pig4cloud.pig.vault.api.enums.AccountType;
import com.pig4cloud.pig.vault.api.enums.LedgerType;
import com.pig4cloud.pig.vault.api.enums.RefType;
import com.pig4cloud.pig.vault.mapper.BalanceMapper;
import com.pig4cloud.pig.vault.mapper.LedgerEntryMapper;
import com.pig4cloud.pig.vault.mapper.VaultAccountMapper;
import com.pig4cloud.pig.vault.mapper.VaultAssetMapper;
import com.pig4cloud.pig.common.security.service.PigUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vault Balance Controller Integration Test with TestContainers
 *
 * @author luka
 * @date 2025-01-23
 */
@SpringBootTest(classes = PigVaultApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VaultBalanceControllerTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
		.withDatabaseName("pig-test")
		.withUsername("test")
		.withPassword("test123456")
		.withInitScript("db/vault_schema_test.sql");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
		registry.add("security.log.enabled", () -> "false");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private BalanceMapper balanceMapper;

	@Autowired
	private LedgerEntryMapper ledgerEntryMapper;

	@Autowired
	private VaultAccountMapper vaultAccountMapper;

	@Autowired
	private VaultAssetMapper vaultAssetMapper;

	@MockBean
	private OpaqueTokenIntrospector opaqueTokenIntrospector;

	private static final Long USER_ID = 10001L;

	private static final Long ACCOUNT_ID = 1001L;

	private static final Long ASSET_ID = 1L;

	private static final String SYMBOL = "USDC";

	private static final BigDecimal INITIAL_AVAILABLE = new BigDecimal("100.000000");

	private static final String TEST_TOKEN = "test-token";

	@BeforeEach
	void setUp() {
		when(opaqueTokenIntrospector.introspect(anyString())).thenReturn(testPigUser());

		// Clean up existing test data
		ledgerEntryMapper.delete(Wrappers.<LedgerEntry>lambdaQuery().eq(LedgerEntry::getAccountId, ACCOUNT_ID));
		balanceMapper.delete(Wrappers.<Balance>lambdaQuery().eq(Balance::getAccountId, ACCOUNT_ID));
		vaultAccountMapper.delete(Wrappers.<VaultAccount>lambdaQuery().eq(VaultAccount::getAccountId, ACCOUNT_ID));
		vaultAssetMapper.delete(Wrappers.<VaultAsset>lambdaQuery().eq(VaultAsset::getAssetId, ASSET_ID));

		// Insert test account
		VaultAccount account = new VaultAccount();
		account.setAccountId(ACCOUNT_ID);
		account.setUserId(USER_ID);
		account.setAccountType(AccountType.USER);
		account.setStatus(AccountStatus.ACTIVE);
		account.setCreateTime(Instant.now());
		account.setUpdateTime(Instant.now());
		vaultAccountMapper.insert(account);

		// Insert test asset (USDC)
		VaultAsset asset = new VaultAsset();
		asset.setAssetId(ASSET_ID);
		asset.setSymbol(SYMBOL);
		asset.setDecimals(6);
		asset.setIsActive(true);
		asset.setCreateTime(Instant.now());
		vaultAssetMapper.insert(asset);

		// Insert test balance (100 USDC available, 0 frozen)
		Balance balance = new Balance();
		balance.setBalanceId(1L);
		balance.setAccountId(ACCOUNT_ID);
		balance.setAssetId(ASSET_ID);
		balance.setAvailable(INITIAL_AVAILABLE);
		balance.setFrozen(BigDecimal.ZERO);
		balance.setVersion(0L);
		balance.setUpdateTime(Instant.now());
		balanceMapper.insert(balance);
	}

	private PigUser testPigUser() {
		return new PigUser(USER_ID, 1L, "testuser", "password", "13800138000", true, true, true, true,
				java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
	}

	/**
	 * Test Case 1: Deposit successfully
	 */
	@Test
	@Order(1)
	@WithMockUser(username = "testuser", roles = "USER")
	@DisplayName("1. Deposit successfully via API")
	@Transactional
	void testDepositSuccess() throws Exception {
		DepositRequest request = new DepositRequest();
		request.setAccountId(ACCOUNT_ID);
		request.setSymbol(SYMBOL);
		request.setAmount(new BigDecimal("50.000000"));
		request.setRefId("API-DEPOSIT-001");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
			.andExpect(jsonPath("$.data.symbol").value(SYMBOL))
			.andExpect(jsonPath("$.data.available").value(150.0))
			.andExpect(jsonPath("$.data.frozen").value(0.0));

		// Verify balance in database
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("150.000000"));

		// Verify ledger entry
		LedgerEntry ledger = ledgerEntryMapper.selectOne(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getRefType, RefType.DEPOSIT)
			.eq(LedgerEntry::getRefId, "API-DEPOSIT-001"));
		assertThat(ledger).isNotNull();
		assertThat(ledger.getEntryType()).isEqualTo(LedgerType.DEPOSIT);
	}

	/**
	 * Test Case 2: Deposit with validation errors
	 */
	@Test
	@Order(2)
	@WithMockUser(username = "testuser", roles = "USER")
	@DisplayName("2. Deposit with validation errors")
	void testDepositValidationErrors() throws Exception {
		// Missing accountId
		DepositRequest request1 = new DepositRequest();
		request1.setSymbol(SYMBOL);
		request1.setAmount(new BigDecimal("10.000000"));
		request1.setRefId("API-DEPOSIT-002");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request1)))
			.andDo(print())
			.andExpect(status().is4xxClientError());

		// Negative amount
		DepositRequest request2 = new DepositRequest();
		request2.setAccountId(ACCOUNT_ID);
		request2.setSymbol(SYMBOL);
		request2.setAmount(new BigDecimal("-10.000000"));
		request2.setRefId("API-DEPOSIT-003");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request2)))
			.andDo(print())
			.andExpect(status().is4xxClientError());

		// Blank refId
		DepositRequest request3 = new DepositRequest();
		request3.setAccountId(ACCOUNT_ID);
		request3.setSymbol(SYMBOL);
		request3.setAmount(new BigDecimal("10.000000"));
		request3.setRefId("");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request3)))
			.andDo(print())
			.andExpect(status().is4xxClientError());
	}

	/**
	 * Test Case 3: Deposit idempotency
	 */
	@Test
	@Order(3)
	@WithMockUser(username = "testuser", roles = "USER")
	@DisplayName("3. Deposit idempotency - same refId")
	@Transactional
	void testDepositIdempotency() throws Exception {
		DepositRequest request = new DepositRequest();
		request.setAccountId(ACCOUNT_ID);
		request.setSymbol(SYMBOL);
		request.setAmount(new BigDecimal("30.000000"));
		request.setRefId("API-DEPOSIT-004");

		// First deposit
		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(130.0));

		// Second deposit with same refId
		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(130.0));

		// Verify balance increased only once
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("130.000000"));
	}

	/**
	 * Test Case 4: Get my balance successfully
	 */
	@Test
	@Order(4)
	@DisplayName("4. Get my balance successfully")
	void testGetMyBalanceSuccess() throws Exception {
		mockMvc.perform(get("/balance/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
			.param("symbol", SYMBOL))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
			.andExpect(jsonPath("$.data.symbol").value(SYMBOL))
			.andExpect(jsonPath("$.data.available").value(100.0))
			.andExpect(jsonPath("$.data.frozen").value(0.0));
	}

	/**
	 * Test Case 5: Get my balance with invalid symbol
	 */
	@Test
	@Order(5)
	@DisplayName("5. Get my balance with invalid symbol")
	void testGetMyBalanceInvalidSymbol() throws Exception {
		mockMvc.perform(get("/balance/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
			.param("symbol", "INVALID"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1))
			.andExpect(jsonPath("$.msg").exists());
	}

	/**
	 * Test Case 6: Deposit and then check balance
	 */
	@Test
	@Order(6)
	@DisplayName("6. Deposit and then check my balance")
	@Transactional
	void testDepositAndCheckMyBalance() throws Exception {
		// Deposit
		DepositRequest depositRequest = new DepositRequest();
		depositRequest.setAccountId(ACCOUNT_ID);
		depositRequest.setSymbol(SYMBOL);
		depositRequest.setAmount(new BigDecimal("75.500000"));
		depositRequest.setRefId("API-DEPOSIT-006");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(depositRequest)))
			.andExpect(status().isOk());

		// Check my balance
		mockMvc.perform(get("/balance/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN)
			.param("symbol", SYMBOL))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(0))
			.andExpect(jsonPath("$.data.available").value(175.5))
			.andExpect(jsonPath("$.data.frozen").value(0.0));
	}

	/**
	 * Test Case 7: Multiple deposits with different refIds
	 */
	@Test
	@Order(7)
	@WithMockUser(username = "testuser", roles = "USER")
	@DisplayName("7. Multiple deposits accumulate correctly")
	@Transactional
	void testMultipleDeposits() throws Exception {
		// First deposit
		DepositRequest request1 = new DepositRequest();
		request1.setAccountId(ACCOUNT_ID);
		request1.setSymbol(SYMBOL);
		request1.setAmount(new BigDecimal("20.000000"));
		request1.setRefId("API-DEPOSIT-007-1");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(120.0));

		// Second deposit
		DepositRequest request2 = new DepositRequest();
		request2.setAccountId(ACCOUNT_ID);
		request2.setSymbol(SYMBOL);
		request2.setAmount(new BigDecimal("30.000000"));
		request2.setRefId("API-DEPOSIT-007-2");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request2)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(150.0));

		// Third deposit
		DepositRequest request3 = new DepositRequest();
		request3.setAccountId(ACCOUNT_ID);
		request3.setSymbol(SYMBOL);
		request3.setAmount(new BigDecimal("50.000000"));
		request3.setRefId("API-DEPOSIT-007-3");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request3)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.available").value(200.0));

		// Verify total balance
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("200.000000"));

		// Verify three ledger entries
		long ledgerCount = ledgerEntryMapper.selectCount(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getEntryType, LedgerType.DEPOSIT));
		assertThat(ledgerCount).isEqualTo(3);
	}

	/**
	 * Test Case 8: Deposit with non-existent account
	 */
	@Test
	@Order(8)
	@WithMockUser(username = "testuser", roles = "USER")
	@DisplayName("8. Deposit with non-existent account")
	void testDepositNonExistentAccount() throws Exception {
		DepositRequest request = new DepositRequest();
		request.setAccountId(99999L); // Non-existent account
		request.setSymbol(SYMBOL);
		request.setAmount(new BigDecimal("10.000000"));
		request.setRefId("API-DEPOSIT-008");

		mockMvc
			.perform(post("/deposit").header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(1))
			.andExpect(jsonPath("$.msg").value("Account not found for accountId=99999"));
	}

}
