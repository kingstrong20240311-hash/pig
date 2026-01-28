/**
 * Standalone k6 script: create assets, markets, users, and deposits.
 *
 * Run once before load tests:
 *   k6 run pig-e2e-test/k6/setup/data-setup.js \
 *     -e K6_BASE_URL=http://localhost:9999 \
 *     -e K6_MARKETS=100 -e K6_USERS=10000
 *
 * Outputs: pig-e2e-test/k6/setup/test-data.json
 */

import { sleep } from 'k6';
import { SharedArray } from 'k6/data';
import {
	BASE_URL,
	MARKET_COUNT,
	USER_COUNT,
	DEPOSIT_AMOUNT,
	SELLER_RATIO,
	SELLER_MARKET_COUNT,
	USER_PREFIX,
	USER_PASSWORD,
	POLL_INTERVAL_MS,
	MAX_POLL_SECONDS,
} from '../config.js';
import { login, encrypt } from '../helpers/auth.js';
import { authGet, authPost } from '../helpers/http.js';

// This script uses a single VU to set up data sequentially.
export const options = {
	vus: 1,
	iterations: 1,
};

export default function () {
	// Login as admin to create assets/markets
	const adminToken = login('admin', '123456');
	if (!adminToken) {
		console.error('Failed to login as admin');
		return;
	}

	const data = {
		markets: [],
		marketDetails: {}, // Store symbolIdYes/No for each market
		users: [],
	};

	// --- Phase 1: Ensure USDC asset exists ---
	console.log('Phase 1: Ensuring USDC asset...');
	ensureAsset(adminToken, 'USDC', 6);

	// --- Phase 2: Create markets ---
	console.log(`Phase 2: Creating ${MARKET_COUNT} markets...`);
	for (let i = 0; i < MARKET_COUNT; i++) {
		const marketId = createMarket(adminToken, i);
		if (marketId) {
			data.markets.push(marketId);
		}
		if ((i + 1) % 10 === 0) {
			console.log(`  Created ${i + 1}/${MARKET_COUNT} markets`);
		}
	}

	// Wait for all markets to become ACTIVE and outcome assets to be generated
	console.log('Waiting for markets to become ACTIVE and outcome assets to be generated...');
	for (const marketId of data.markets) {
		const marketDetail = waitForMarketReady(adminToken, marketId);
		if (marketDetail) {
			data.marketDetails[marketId] = marketDetail;
			console.log(`  Market ${marketId}: symbolIdYes=${marketDetail.symbolIdYes}, symbolIdNo=${marketDetail.symbolIdNo}`);
		}
	}

	// --- Phase 3: Register users ---
	console.log(`Phase 3: Registering ${USER_COUNT} users...`);
	for (let i = 0; i < USER_COUNT; i++) {
		const username = `${USER_PREFIX}${i}`;
		const user = registerUser(adminToken, username, USER_PASSWORD);
		if (user) {
			data.users.push({ username, password: USER_PASSWORD, userId: user.userId });
		}
		if ((i + 1) % 100 === 0) {
			console.log(`  Registered ${i + 1}/${USER_COUNT} users`);
		}
	}

	// --- Phase 4: Deposit USDC to all users ---
	console.log(`Phase 4: Depositing USDC to ${data.users.length} users...`);
	for (let i = 0; i < data.users.length; i++) {
		const user = data.users[i];
		deposit(adminToken, user.userId, 'USDC', DEPOSIT_AMOUNT);
		if ((i + 1) % 100 === 0) {
			console.log(`  Deposited to ${i + 1}/${data.users.length} users`);
		}
	}

	// --- Phase 5: Deposit outcome assets to seller subset ---
	const sellerCount = Math.floor(data.users.length * SELLER_RATIO);
	const marketsForSellers = Math.min(SELLER_MARKET_COUNT, data.markets.length);
	console.log(`Phase 5: Depositing outcome assets to ${sellerCount} sellers across ${marketsForSellers} markets...`);

	for (let i = 0; i < sellerCount; i++) {
		const user = data.users[i];
		for (let m = 0; m < marketsForSellers; m++) {
			const marketId = data.markets[m];
			deposit(adminToken, user.userId, `M${marketId}_YES`, DEPOSIT_AMOUNT);
			deposit(adminToken, user.userId, `M${marketId}_NO`, DEPOSIT_AMOUNT);
		}
		if ((i + 1) % 10 === 0) {
			console.log(`  Seller deposits: ${i + 1}/${sellerCount}`);
		}
	}

	// Output data as console JSON (capture with k6 --out or redirect)
	console.log('=== TEST_DATA_JSON_START ===');
	console.log(JSON.stringify(data));
	console.log('=== TEST_DATA_JSON_END ===');
	console.log(`Setup complete. Markets: ${data.markets.length}, Users: ${data.users.length}`);
}

// --- Helper functions ---

function ensureAsset(token, symbol, decimals) {
	const res = authGet(`/vault/asset/symbol/${symbol}`, token);
	if (res.status === 200) {
		const body = res.json();
		if (body && body.code === 0 && body.data) {
			console.log(`  Asset ${symbol} already exists`);
			return;
		}
	}
	const createRes = authPost('/vault/asset', token, {
		symbol: symbol,
		decimals: decimals,
		isActive: true,
	});
	if (createRes.status === 200) {
		console.log(`  Asset ${symbol} created`);
	} else {
		console.error(`  Failed to create asset ${symbol}: ${createRes.status}`);
	}
}

function createMarket(token, index) {
	const res = authPost('/order/market', token, {
		name: `k6-load-test-market-${Date.now()}-${index}`,
		status: 'ACTIVE',
		expireAt: Date.now() + 86400 * 365 * 1000,
	});
	if (res.status === 200) {
		const body = res.json();
		if (body && body.data) {
			return typeof body.data === 'object' ? body.data.id || body.data.marketId : body.data;
		}
	}
	console.error(`  Failed to create market ${index}: ${res.status}`);
	return null;
}

function waitForMarketReady(token, marketId) {
	const maxAttempts = (MAX_POLL_SECONDS * 1000) / POLL_INTERVAL_MS;
	for (let i = 0; i < maxAttempts; i++) {
		const res = authGet(`/order/market/${marketId}`, token);
		if (res.status === 200) {
			const body = res.json();
			if (
				body &&
				body.data &&
				body.data.status === 'ACTIVE' &&
				body.data.symbolIdYes != null &&
				body.data.symbolIdNo != null
			) {
				// Return market details including outcome asset IDs
				return {
					marketId: marketId,
					status: body.data.status,
					symbolIdYes: body.data.symbolIdYes,
					symbolIdNo: body.data.symbolIdNo,
					// Also include symbol strings for convenience
					symbolYes: `M${marketId}_YES`,
					symbolNo: `M${marketId}_NO`,
				};
			}
		}
		sleep(POLL_INTERVAL_MS / 1000);
	}
	console.warn(`  Market ${marketId} did not become ready in time (outcome assets not generated)`);
	return null;
}

function registerUser(token, username, password) {
	const res = authPost('/auth/register', token, {
		username: username,
		password: password,
	});
	if (res.status === 200) {
		const body = res.json();
		if (body && body.code === 0) {
			return body.data || { userId: username };
		}
	}
	// User may already exist, try to get userId by logging in
	return { userId: username };
}

function deposit(token, userId, symbol, amount) {
	const res = authPost('/vault/deposit', token, {
		userId: userId,
		symbol: symbol,
		amount: amount,
		refId: `k6-deposit-${userId}-${symbol}-${Date.now()}`,
	});
	if (res.status !== 200) {
		console.warn(`  Deposit failed for ${userId}/${symbol}: ${res.status}`);
	}
}
