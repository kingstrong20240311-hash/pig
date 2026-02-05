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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.vault.api.dto.BalanceResponse;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Vault Balance Service Integration Test with TestContainers
 *
 * @author luka
 * @date 2025-01-23
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VaultBalanceServiceTest {

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
	}

	@Autowired
	private VaultBalanceService vaultBalanceService;

	@Autowired
	private BalanceMapper balanceMapper;

	@Autowired
	private LedgerEntryMapper ledgerEntryMapper;

	@Autowired
	private VaultAccountMapper vaultAccountMapper;

	@Autowired
	private VaultAssetMapper vaultAssetMapper;

	private static final Long USER_ID = 10001L;

	private static final Long ACCOUNT_ID = 1001L;

	private static final Long ASSET_ID = 1L;

	private static final String SYMBOL = "USDC";

	private static final BigDecimal INITIAL_AVAILABLE = new BigDecimal("100.000000");

	@BeforeEach
	void setUp() {
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

	/**
	 * Test Case 1: Deposit successfully
	 */
	@Test
	@Order(1)
	@DisplayName("1. Deposit funds successfully")
	@Transactional
	void testDepositSuccess() {
		// Given
		DepositRequest request = new DepositRequest();
		request.setUserId(USER_ID);
		request.setSymbol(SYMBOL);
		request.setAmount(new BigDecimal("50.000000"));
		request.setRefId("DEPOSIT-001");

		// When
		BalanceResponse response = vaultBalanceService.deposit(request);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getAccountId()).isEqualTo(String.valueOf(ACCOUNT_ID));
		assertThat(response.getSymbol()).isEqualTo(SYMBOL);
		assertThat(response.getAvailable()).isEqualByComparingTo(new BigDecimal("150.000000"));
		assertThat(response.getFrozen()).isEqualByComparingTo(BigDecimal.ZERO);

		// Verify balance in database
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("150.000000"));
		assertThat(balance.getVersion()).isEqualTo(1L);

		// Verify ledger entry created
		LedgerEntry ledger = ledgerEntryMapper.selectOne(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getRefType, RefType.DEPOSIT)
			.eq(LedgerEntry::getRefId, "DEPOSIT-001"));
		assertThat(ledger).isNotNull();
		assertThat(ledger.getEntryType()).isEqualTo(LedgerType.DEPOSIT);
		assertThat(ledger.getAmount()).isEqualByComparingTo(new BigDecimal("50.000000"));
		assertThat(ledger.getBeforeAvailable()).isEqualByComparingTo(INITIAL_AVAILABLE);
		assertThat(ledger.getAfterAvailable()).isEqualByComparingTo(new BigDecimal("150.000000"));
	}

	/**
	 * Test Case 2: Deposit idempotency
	 */
	@Test
	@Order(2)
	@DisplayName("2. Deposit idempotency - same refId")
	@Transactional
	void testDepositIdempotency() {
		// Given
		DepositRequest request = new DepositRequest();
		request.setUserId(USER_ID);
		request.setSymbol(SYMBOL);
		request.setAmount(new BigDecimal("30.000000"));
		request.setRefId("DEPOSIT-002");

		// First deposit
		BalanceResponse response1 = vaultBalanceService.deposit(request);
		assertThat(response1.getAvailable()).isEqualByComparingTo(new BigDecimal("130.000000"));

		// Second deposit with same refId (should be idempotent)
		BalanceResponse response2 = vaultBalanceService.deposit(request);
		assertThat(response2.getAvailable()).isEqualByComparingTo(new BigDecimal("130.000000"));

		// Verify balance increased only once
		Balance balance = balanceMapper.selectOne(Wrappers.<Balance>lambdaQuery()
			.eq(Balance::getAccountId, ACCOUNT_ID)
			.eq(Balance::getAssetId, ASSET_ID));
		assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("130.000000"));

		// Verify only one ledger entry
		long ledgerCount = ledgerEntryMapper.selectCount(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getRefType, RefType.DEPOSIT)
			.eq(LedgerEntry::getRefId, "DEPOSIT-002"));
		assertThat(ledgerCount).isEqualTo(1);
	}

	/**
	 * Test Case 3: Deposit with invalid symbol
	 */
	@Test
	@Order(3)
	@DisplayName("3. Deposit with invalid symbol")
	void testDepositInvalidSymbol() {
		// Given
		DepositRequest request = new DepositRequest();
		request.setUserId(USER_ID);
		request.setSymbol("INVALID");
		request.setAmount(new BigDecimal("10.000000"));
		request.setRefId("DEPOSIT-003");

		// When & Then
		assertThatThrownBy(() -> vaultBalanceService.deposit(request)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Asset not found or inactive");
	}

	/**
	 * Test Case 4: Deposit with invalid account
	 */
	@Test
	@Order(4)
	@DisplayName("4. Deposit with non-existent account")
	void testDepositInvalidAccount() {
		// Given
		DepositRequest request = new DepositRequest();
		request.setUserId(99999L); // Non-existent user
		request.setSymbol(SYMBOL);
		request.setAmount(new BigDecimal("10.000000"));
		request.setRefId("DEPOSIT-004");

		// When & Then
		assertThatThrownBy(() -> vaultBalanceService.deposit(request)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Account not found");
	}

	/**
	 * Test Case 5: Multiple deposits with different refIds
	 */
	@Test
	@Order(5)
	@DisplayName("5. Multiple deposits with different refIds")
	@Transactional
	void testMultipleDeposits() {
		// First deposit
		DepositRequest request1 = new DepositRequest();
		request1.setUserId(USER_ID);
		request1.setSymbol(SYMBOL);
		request1.setAmount(new BigDecimal("20.000000"));
		request1.setRefId("DEPOSIT-005-1");
		vaultBalanceService.deposit(request1);

		// Second deposit
		DepositRequest request2 = new DepositRequest();
		request2.setUserId(USER_ID);
		request2.setSymbol(SYMBOL);
		request2.setAmount(new BigDecimal("30.000000"));
		request2.setRefId("DEPOSIT-005-2");
		vaultBalanceService.deposit(request2);

		// Third deposit
		DepositRequest request3 = new DepositRequest();
		request3.setUserId(USER_ID);
		request3.setSymbol(SYMBOL);
		request3.setAmount(new BigDecimal("50.000000"));
		request3.setRefId("DEPOSIT-005-3");
		BalanceResponse response3 = vaultBalanceService.deposit(request3);

		// Verify total balance
		assertThat(response3.getAvailable()).isEqualByComparingTo(new BigDecimal("200.000000"));

		// Verify three ledger entries
		long ledgerCount = ledgerEntryMapper.selectCount(Wrappers.<LedgerEntry>lambdaQuery()
			.eq(LedgerEntry::getAccountId, ACCOUNT_ID)
			.eq(LedgerEntry::getEntryType, LedgerType.DEPOSIT));
		assertThat(ledgerCount).isEqualTo(3);
	}

	/**
	 * Test Case 6: Get balance for current user successfully
	 */
	@Test
	@Order(6)
	@DisplayName("6. Get balance for current user successfully")
	void testGetMyBalanceSuccess() {
		// Mock security context
		mockSecurityContext(USER_ID);

		// When
		BalanceResponse response = vaultBalanceService.getMyBalance(SYMBOL);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getAccountId()).isEqualTo(String.valueOf(ACCOUNT_ID));
		assertThat(response.getSymbol()).isEqualTo(SYMBOL);
		assertThat(response.getAvailable()).isEqualByComparingTo(INITIAL_AVAILABLE);
		assertThat(response.getFrozen()).isEqualByComparingTo(BigDecimal.ZERO);

		// Cleanup
		SecurityContextHolder.clearContext();
	}

	/**
	 * Test Case 7: Get balance without authentication
	 */
	@Test
	@Order(7)
	@DisplayName("7. Get balance without authentication")
	void testGetMyBalanceNotAuthenticated() {
		// Clear security context
		SecurityContextHolder.clearContext();

		// When & Then
		assertThatThrownBy(() -> vaultBalanceService.getMyBalance(SYMBOL)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("User not authenticated");
	}

	/**
	 * Test Case 8: Get balance for user without account
	 */
	@Test
	@Order(8)
	@DisplayName("8. Get balance for user without account")
	void testGetMyBalanceNoAccount() {
		// Mock security context with non-existent user
		mockSecurityContext(99999L);

		// When & Then
		assertThatThrownBy(() -> vaultBalanceService.getMyBalance(SYMBOL)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Account not found");

		// Cleanup
		SecurityContextHolder.clearContext();
	}

	/**
	 * Test Case 9: Get balance with invalid symbol
	 */
	@Test
	@Order(9)
	@DisplayName("9. Get balance with invalid symbol")
	void testGetMyBalanceInvalidSymbol() {
		// Mock security context
		mockSecurityContext(USER_ID);

		// When & Then
		assertThatThrownBy(() -> vaultBalanceService.getMyBalance("INVALID"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Asset not found or inactive");

		// Cleanup
		SecurityContextHolder.clearContext();
	}

	/**
	 * Test Case 10: Deposit and then check balance
	 */
	@Test
	@Order(10)
	@DisplayName("10. Deposit and then check balance")
	@Transactional
	void testDepositAndCheckBalance() {
		// Mock security context
		mockSecurityContext(USER_ID);

		// Deposit
		DepositRequest depositRequest = new DepositRequest();
		depositRequest.setUserId(USER_ID);
		depositRequest.setSymbol(SYMBOL);
		depositRequest.setAmount(new BigDecimal("75.500000"));
		depositRequest.setRefId("DEPOSIT-010");
		vaultBalanceService.deposit(depositRequest);

		// Check balance
		BalanceResponse response = vaultBalanceService.getMyBalance(SYMBOL);

		// Verify
		assertThat(response.getAvailable()).isEqualByComparingTo(new BigDecimal("175.500000"));
		assertThat(response.getFrozen()).isEqualByComparingTo(BigDecimal.ZERO);

		// Cleanup
		SecurityContextHolder.clearContext();
	}

	/**
	 * Helper method to mock security context with a user
	 */
	private void mockSecurityContext(Long userId) {
		PigUser user = new PigUser(userId, 1L, "testuser", "password", "13800138000", true, true, true, true,
				new ArrayList<>());

		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(user);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);

		SecurityContextHolder.setContext(securityContext);
	}

}
