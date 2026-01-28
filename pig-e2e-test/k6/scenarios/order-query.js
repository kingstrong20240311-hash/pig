import { Trend, Counter } from 'k6/metrics';
import { login } from '../helpers/auth.js';
import { authPost, authGet } from '../helpers/http.js';
import { checkSuccess } from '../checks/validators.js';
import { USER_PASSWORD } from '../config.js';

export const orderQueryDuration = new Trend('order_query_duration', true);
export const orderQueryErrors = new Counter('order_query_errors');

/**
 * Create an order then query it by ID.
 */
export default function orderQuery(sharedData) {
	const user = pickRandom(sharedData.users);
	const marketId = pickRandom(sharedData.markets);
	const token = login(user.username, user.password || USER_PASSWORD);
	if (!token) {
		orderQueryErrors.add(1);
		return;
	}

	// Create an order to query
	const createRes = authPost('/order/create', token, {
		marketId: marketId,
		outcome: 'YES',
		side: 'BUY',
		type: 'LIMIT',
		quantity: 10,
		price: 0.5,
		timeInForce: 'GTC',
		idempotencyKey: generateUUID(),
	});

	const body = createRes.json();
	if (!body || !body.data || !body.data.orderId) {
		orderQueryErrors.add(1);
		return;
	}

	const orderId = body.data.orderId;

	// Query the order
	const queryRes = authGet(`/order/${orderId}`, token);
	orderQueryDuration.add(queryRes.timings.duration);

	if (!checkSuccess(queryRes, 'order query')) {
		orderQueryErrors.add(1);
	}
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
