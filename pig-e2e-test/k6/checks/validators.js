import { check } from 'k6';

/**
 * Validate a successful API response (status 200, code 0).
 */
export function checkSuccess(res, name) {
	return check(res, {
		[`${name} status is 200`]: (r) => r.status === 200,
		[`${name} code is 0`]: (r) => {
			const body = r.json();
			return body && body.code === 0;
		},
	});
}

/**
 * Validate order creation response has orderId.
 */
export function checkOrderCreated(res) {
	return check(res, {
		'order create status 200': (r) => r.status === 200,
		'order create has orderId': (r) => {
			const body = r.json();
			return body && body.data && body.data.orderId;
		},
	});
}

/**
 * Validate order status matches expected.
 */
export function checkOrderStatus(res, expectedStatus) {
	return check(res, {
		[`order status is ${expectedStatus}`]: (r) => {
			const body = r.json();
			return body && body.data && body.data.status === expectedStatus;
		},
	});
}

/**
 * Validate balance is non-negative.
 */
export function checkBalanceValid(res) {
	return check(res, {
		'balance status 200': (r) => r.status === 200,
		'balance available >= 0': (r) => {
			const body = r.json();
			return body && body.data && parseFloat(body.data.available) >= 0;
		},
	});
}

/**
 * Validate freeze record exists with expected status.
 */
export function checkFreezeStatus(res, expectedStatus) {
	return check(res, {
		[`freeze status is ${expectedStatus}`]: (r) => {
			const body = r.json();
			return body && body.data && body.data.status === expectedStatus;
		},
	});
}
