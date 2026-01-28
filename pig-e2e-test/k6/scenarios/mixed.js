import orderCreate from './order-create.js';
import orderCancel from './order-cancel.js';

// Re-export metrics so they're registered
export { orderCreateDuration, orderCreateErrors } from './order-create.js';
export { orderCancelDuration, orderCancelErrors } from './order-cancel.js';

const CREATE_WEIGHT = parseFloat(__ENV.K6_CREATE_WEIGHT || '0.7');

/**
 * Mixed scenario: weighted split between order create and order cancel.
 * Default: 70% create, 30% cancel.
 */
export default function mixed(sharedData) {
	if (Math.random() < CREATE_WEIGHT) {
		orderCreate(sharedData);
	} else {
		orderCancel(sharedData);
	}
}
