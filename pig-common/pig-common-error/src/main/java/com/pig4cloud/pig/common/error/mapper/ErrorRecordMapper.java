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

package com.pig4cloud.pig.common.error.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.common.error.entity.ErrorRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;

/**
 * 错误记录Mapper
 *
 * @author pig4cloud
 * @date 2026-01-21
 */
@Mapper
public interface ErrorRecordMapper extends BaseMapper<ErrorRecord> {

	/**
	 * 查询待补偿的错误记录
	 * @param domain 领域
	 * @param limit 限制数量
	 * @param now 当前时间
	 * @return 错误记录列表
	 */
	@Select("<script>" + "SELECT * FROM error_record " + "WHERE status IN ('NEW', 'RETRYING') "
			+ "<if test='domain != null and domain != \"\"'>" + "AND domain = #{domain} " + "</if>"
			+ "AND (next_retry_time IS NULL OR next_retry_time &lt;= #{now}) " + "ORDER BY created_at ASC "
			+ "LIMIT #{limit}" + "</script>")
	List<ErrorRecord> selectPendingRecords(@Param("domain") String domain, @Param("limit") int limit,
			@Param("now") Instant now);

}
