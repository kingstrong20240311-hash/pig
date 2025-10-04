/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pig4cloud.pig.smm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.smm.entity.ApiHealthStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * API健康状态Mapper
 *
 * @author pig4cloud
 * @date 2025-09-23
 */
@Mapper
public interface ApiHealthStatusMapper extends BaseMapper<ApiHealthStatus> {

	/**
	 * 根据API名称查询健康状态
	 *
	 * @param apiName API名称
	 * @return 健康状态
	 */
	@Select("SELECT * FROM api_health_status WHERE api_name = #{apiName}")
	ApiHealthStatus selectByApiName(@Param("apiName") String apiName);

	/**
	 * 更新API健康状态
	 *
	 * @param apiName API名称
	 * @param isAvailable 是否可用
	 * @param responseTime 响应时间
	 * @param errorMessage 错误信息
	 */
	@Update("UPDATE api_health_status SET is_available = #{isAvailable}, " +
			"last_check_time = NOW(), response_time = #{responseTime}, " +
			"error_message = #{errorMessage}, " +
			"consecutive_failures = CASE WHEN #{isAvailable} = 1 THEN 0 ELSE consecutive_failures + 1 END " +
			"WHERE api_name = #{apiName}")
	void updateHealthStatus(@Param("apiName") String apiName, @Param("isAvailable") Boolean isAvailable,
							@Param("responseTime") Long responseTime, @Param("errorMessage") String errorMessage);

}