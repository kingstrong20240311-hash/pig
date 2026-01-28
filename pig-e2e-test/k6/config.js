// k6 load test configuration
// All settings can be overridden via environment variables (k6 -e KEY=VALUE)

export const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:9999';
export const CLIENT_ID = __ENV.K6_CLIENT_ID || 'test';
export const CLIENT_SECRET = __ENV.K6_CLIENT_SECRET || 'test';
export const ENCODE_KEY = __ENV.K6_ENCODE_KEY || 'thanks,pig4cloud';

export const VUS = parseInt(__ENV.K6_VUS || '50');
export const DURATION = __ENV.K6_DURATION || '40m';

// Threshold defaults (milliseconds / rate)
export const P95_MS = parseInt(__ENV.K6_P95_MS || '500');
export const P99_MS = parseInt(__ENV.K6_P99_MS || '1000');
export const ERROR_RATE = parseFloat(__ENV.K6_ERROR_RATE || '0.01');

// Data setup defaults
export const MARKET_COUNT = parseInt(__ENV.K6_MARKETS || '100');
export const USER_COUNT = parseInt(__ENV.K6_USERS || '10000');
export const DEPOSIT_AMOUNT = parseFloat(__ENV.K6_DEPOSIT_AMOUNT || '100000');
export const SELLER_RATIO = parseFloat(__ENV.K6_SELLER_RATIO || '0.1');
export const SELLER_MARKET_COUNT = parseInt(__ENV.K6_SELLER_MARKETS || '10');

// Test user credentials template
export const USER_PREFIX = __ENV.K6_USER_PREFIX || 'k6user';
export const USER_PASSWORD = __ENV.K6_USER_PASSWORD || '123456';

// Polling configuration for async operations
// Used for waiting for market status (ACTIVE) and outcome assets (YES/NO) to be generated
export const POLL_INTERVAL_MS = parseInt(__ENV.K6_POLL_INTERVAL_MS || '500');
export const MAX_POLL_SECONDS = parseInt(__ENV.K6_MAX_POLL_SECONDS || '30');

// Ramping stages
export function getStages() {
	const custom = __ENV.K6_STAGES;
	if (custom) {
		// Format: "duration:target,duration:target,..."
		return custom.split(',').map((s) => {
			const [duration, target] = s.split(':');
			return { duration, target: parseInt(target) };
		});
	}
	return [
		{ duration: '5m', target: VUS }, // warmup
		{ duration: '20m', target: VUS }, // steady
		{ duration: '10m', target: VUS * 2 }, // peak
		{ duration: '5m', target: 0 }, // cooldown
	];
}

// Thresholds object for k6 options
export function getThresholds() {
	return {
		http_req_duration: [`p(95)<${P95_MS}`, `p(99)<${P99_MS}`],
		http_req_failed: [`rate<${ERROR_RATE}`],
		order_create_duration: [`p(95)<${P95_MS}`],
		order_cancel_duration: [`p(95)<${P95_MS}`],
		order_query_duration: [`p(95)<${P95_MS}`],
		vault_balance_duration: [`p(95)<${P95_MS}`],
	};
}
