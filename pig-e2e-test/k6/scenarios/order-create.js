import { Trend, Counter } from 'k6/metrics';
import { login } from '../helpers/auth.js';
import { authPost } from '../helpers/http.js';
import { checkOrderCreated } from '../checks/validators.js';
import { USER_PASSWORD } from '../config.js';

export const orderCreateDuration = new Trend('order_create_duration', true);
export const orderCreateErrors = new Counter('order_create_errors');

/**
 * Create a random LIMIT order.
 * @param {object} sharedData - { users: [...], markets: [...] }
 */
export default function orderCreate(sharedData) {
	const user = pickRandom(sharedData.users);
	const marketId = pickRandom(sharedData.markets);
	const token = login(user.username, user.password || USER_PASSWORD);
	if (!token) {
		orderCreateErrors.add(1);
		return null;
	}

	const side = Math.random() < 0.5 ? 'BUY' : 'SELL';
	const outcome = Math.random() < 0.5 ? 'YES' : 'NO';
	const price = (Math.floor(Math.random() * 8) + 1) / 10; // 0.1 - 0.9
	const quantity = Math.floor(Math.random() * 100) + 10; // 10 - 109

	const body = {
		marketId: marketId,
		outcome: outcome,
		side: side,
		type: 'LIMIT',
		quantity: quantity,
		price: price,
		timeInForce: 'GTC',
		idempotencyKey: generateUUID(),
	};

	const res = authPost('/order/create', token, body);
	orderCreateDuration.add(res.timings.duration);

	if (!checkOrderCreated(res)) {
		orderCreateErrors.add(1);
		return null;
	}

	const responseBody = res.json();
	return responseBody.data ? responseBody.data.orderId : null;
}

function pickRandom(arr) {
	return arr[Math.floor(Math.random() * arr.length)];
}

function generateUUID() {
	// Simple UUID v4
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
		const r = (Math.random() * 16) | 0;
		const v = c === 'x' ? r : (r & 0x3) | 0x8;
		return v.toString(16);
	});
}
