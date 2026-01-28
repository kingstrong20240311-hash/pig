/**
 * Main k6 load test entrypoint.
 *
 * Usage:
 *   k6 run pig-e2e-test/k6/run.js \
 *     -e K6_BASE_URL=http://localhost:9999 \
 *     -e K6_VUS=200 -e K6_P95_MS=500
 *
 * Requires test-data.json from data-setup.js (or pass inline via K6_TEST_DATA env).
 */

import { SharedArray } from 'k6/data';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';
import { getStages, getThresholds, VUS } from './config.js';

// Import scenario functions
import orderCreate from './scenarios/order-create.js';
import orderCancel from './scenarios/order-cancel.js';
import orderQuery from './scenarios/order-query.js';
import vaultBalance from './scenarios/vault-balance.js';
import mixed from './scenarios/mixed.js';

// Re-export all custom metrics
export { orderCreateDuration, orderCreateErrors } from './scenarios/order-create.js';
export { orderCancelDuration, orderCancelErrors } from './scenarios/order-cancel.js';
export { orderQueryDuration, orderQueryErrors } from './scenarios/order-query.js';
export { vaultBalanceDuration, vaultBalanceErrors } from './scenarios/vault-balance.js';

// Load test data prepared by data-setup.js
const testData = JSON.parse(open('./setup/test-data.json'));

export const options = {
	scenarios: {
		mixed_load: {
			executor: 'ramping-vus',
			exec: 'mixedScenario',
			stages: getStages(),
			startVUs: 0,
		},
		read_orders: {
			executor: 'constant-vus',
			exec: 'queryScenario',
			vus: Math.max(1, Math.floor(VUS * 0.2)),
			duration: '40m',
			startTime: '5m', // start after warmup begins
		},
		read_balances: {
			executor: 'constant-vus',
			exec: 'balanceScenario',
			vus: Math.max(1, Math.floor(VUS * 0.1)),
			duration: '40m',
			startTime: '5m',
		},
	},
	thresholds: getThresholds(),
};

// Scenario executor functions
export function mixedScenario() {
	mixed(testData);
}

export function queryScenario() {
	orderQuery(testData);
}

export function balanceScenario() {
	vaultBalance(testData);
}

// JSON + text summary output
export function handleSummary(data) {
	return {
		stdout: textSummary(data, { indent: ' ', enableColors: true }),
		'./k6-summary.json': JSON.stringify(data, null, 2),
	};
}
