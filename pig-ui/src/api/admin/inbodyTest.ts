import request from '/@/utils/request';

export const fetchList = (params: any) => {
	return request({
		url: '/admin/gym/inbody-test/page',
		method: 'get',
		params,
	});
};

export const getObj = (id: number | string) => {
	return request({
		url: `/admin/gym/inbody-test/details/${id}`,
		method: 'get',
	});
};

export const addObj = (obj: any) => {
	return request({
		url: '/admin/gym/inbody-test',
		method: 'post',
		data: obj,
	});
};

export const putObj = (obj: any) => {
	return request({
		url: '/admin/gym/inbody-test',
		method: 'put',
		data: obj,
	});
};

export const delObj = (ids: number[]) => {
	return request({
		url: '/admin/gym/inbody-test',
		method: 'delete',
		data: ids,
	});
};
