import { Trend, Counter } from 'k6/metrics';
import { login } from '../helpers/auth.js';
import { authGet } from '../helpers/http.js';
import { checkBalanceValid } from '../checks/validators.js';
import { USER_PASSWORD } from '../config.js';

export const vaultBalanceDuration = new Trend('vault_balance_duration', true);
export const vaultBalanceErrors = new Counter('vault_balance_errors');

/**
 * Query USDC balance and a random outcome asset balance.
 */
export default function vaultBalance(sharedData) {
	const user = pickRandom(sharedData.users);
	const token = login(user.username, user.password || USER_PASSWORD);
	if (!token) {
		vaultBalanceErrors.add(1);
		return;
	}

	// Query USDC balance
	const usdcRes = authGet(`/vault/balance?accountId=${user.userId}&symbol=USDC`, token);
	vaultBalanceDuration.add(usdcRes.timings.duration);

	if (!checkBalanceValid(usdcRes)) {
		vaultBalanceErrors.add(1);
	}

	// Query a random outcome asset balance
	if (sharedData.markets.length > 0) {
		const marketId = pickRandom(sharedData.markets);
		const outcome = Math.random() < 0.5 ? 'YES' : 'NO';
		const symbol = `M${marketId}_${outcome}`;

		const outcomeRes = authGet(`/vault/balance?accountId=${user.userId}&symbol=${symbol}`, token);
		vaultBalanceDuration.add(outcomeRes.timings.duration);

		if (!checkBalanceValid(outcomeRes)) {
			vaultBalanceErrors.add(1);
		}
	}
}

function pickRandom(arr) {
	return arr[Math.floor(Math.random() * arr.length)];
}
