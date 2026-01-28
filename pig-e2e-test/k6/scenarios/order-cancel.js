import { Trend, Counter } from 'k6/metrics';
import { sleep } from 'k6';
import { login } from '../helpers/auth.js';
import { authPost, authGet } from '../helpers/http.js';
import { checkOrderCreated, checkOrderStatus } from '../checks/validators.js';
import { USER_PASSWORD, POLL_INTERVAL_MS, MAX_POLL_SECONDS } from '../config.js';

export const orderCancelDuration = new Trend('order_cancel_duration', true);
export const orderCancelErrors = new Counter('order_cancel_errors');

/**
 * Create an order then cancel it, verify CANCELLED status.
 */
export default function orderCancel(sharedData) {
	const user = pickRandom(sharedData.users);
	const marketId = pickRandom(sharedData.markets);
	const token = login(user.username, user.password || USER_PASSWORD);
	if (!token) {
		orderCancelErrors.add(1);
		return;
	}

	// Create a LIMIT BUY order (likely won't match immediately)
	const createRes = authPost('/order/create', token, {
		marketId: marketId,
		outcome: 'YES',
		side: 'BUY',
		type: 'LIMIT',
		quantity: 10,
		price: 0.1, // low price, unlikely to match
		timeInForce: 'GTC',
		idempotencyKey: generateUUID(),
	});

	if (!checkOrderCreated(createRes)) {
		orderCancelErrors.add(1);
		return;
	}

	const orderId = createRes.json().data.orderId;

	// Cancel the order
	const cancelRes = authPost('/order/cancel', token, {
		orderId: orderId,
		idempotencyKey: generateUUID(),
	});

	orderCancelDuration.add(cancelRes.timings.duration);

	if (cancelRes.status !== 200) {
		orderCancelErrors.add(1);
		return;
	}

	// Poll for CANCELLED status
	const maxAttempts = (MAX_POLL_SECONDS * 1000) / POLL_INTERVAL_MS;
	for (let i = 0; i < maxAttempts; i++) {
		const queryRes = authGet(`/order/${orderId}`, token);
		if (checkOrderStatus(queryRes, 'CANCELLED')) {
			return;
		}
		sleep(POLL_INTERVAL_MS / 1000);
	}
	orderCancelErrors.add(1);
}

function pickRandom(arr) {
	return arr[Math.floor(Math.random() * arr.length)];
}

function generateUUID() {
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
		const r = (Math.random() * 16) | 0;
		const v = c === 'x' ? r : (r & 0x3) | 0x8;
		return v.toString(16);
	});
}
