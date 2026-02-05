import http from 'k6/http';
import { BASE_URL } from '../config.js';

/**
 * Authenticated GET request.
 * @param {string} path - URL path (appended to BASE_URL)
 * @param {string} token - Bearer token
 * @param {object} [params] - k6 request params (query params go in params.tags or as URL query)
 */
export function authGet(path, token, params) {
	const opts = buildOpts(token, path, params);
	return http.get(`${BASE_URL}${path}`, opts);
}

/**
 * Authenticated POST request with JSON body.
 * @param {string} path - URL path
 * @param {string} token - Bearer token
 * @param {object} body - JSON body
 * @param {object} [params] - extra k6 params
 */
export function authPost(path, token, body, params) {
	const opts = buildOpts(token, path, params);
	return http.post(`${BASE_URL}${path}`, JSON.stringify(body), opts);
}

function buildOpts(token, path, extra) {
	const tag = path.replace(/\/\d+/g, '/{id}'); // normalize IDs for metrics grouping
	const opts = {
		headers: {
			Authorization: `Bearer ${token}`,
			'Content-Type': 'application/json',
			Accept: 'application/json',
		},
		tags: { name: tag },
	};
	if (extra) {
		Object.assign(opts.tags, extra.tags);
		if (extra.headers) Object.assign(opts.headers, extra.headers);
	}
	return opts;
}
