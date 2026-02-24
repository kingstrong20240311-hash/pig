import request from '/@/utils/request';

export function fetchList(query?: Object) {
	return request({
		url: '/admin/gym/trainingsession/page',
		method: 'get',
		params: query,
	});
}

export function addObj(obj?: Object) {
	return request({
		url: '/admin/gym/trainingsession',
		method: 'post',
		data: obj,
	});
}

export function getObj(id?: string) {
	return request({
		url: '/admin/gym/trainingsession/details/' + id,
		method: 'get',
	});
}

export function delObj(ids?: Object) {
	return request({
		url: '/admin/gym/trainingsession',
		method: 'delete',
		data: ids,
	});
}

export function putObj(obj?: Object) {
	return request({
		url: '/admin/gym/trainingsession',
		method: 'put',
		data: obj,
	});
}
