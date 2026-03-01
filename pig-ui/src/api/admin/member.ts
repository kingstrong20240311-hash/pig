import request from '/@/utils/request';

export function fetchList(query?: Object) {
	return request({
		url: '/admin/gym/member/page',
		method: 'get',
		params: query,
	});
}

export function addObj(obj?: Object) {
	return request({
		url: '/admin/gym/member',
		method: 'post',
		data: obj,
	});
}

export function getObj(id?: string) {
	return request({
		url: '/admin/gym/member/details/' + id,
		method: 'get',
	});
}

export function delObj(ids?: Object) {
	return request({
		url: '/admin/gym/member',
		method: 'delete',
		data: ids,
	});
}

export function putObj(obj?: Object) {
	return request({
		url: '/admin/gym/member',
		method: 'put',
		data: obj,
	});
}
