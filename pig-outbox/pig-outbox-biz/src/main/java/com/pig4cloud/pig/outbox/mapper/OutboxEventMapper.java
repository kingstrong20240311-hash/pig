/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pig.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.outbox.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;

/**
 * Outbox事件Mapper
 *
 * @author pig4cloud
 * @date 2025-01-15
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

	/**
	 * 查询待发送事件（保序：每个aggregateId只取最小id的一条）
	 * @param limit 限制数量
	 * @param lockTimeout 锁超时时间（秒）
	 * @return 待发送事件列表
	 */
	@Select("""
			SELECT *
			FROM (
			  SELECT e.*,
			         ROW_NUMBER() OVER (PARTITION BY e.aggregate_type, e.aggregate_id ORDER BY e.id) AS rn
			  FROM outbox_event e
			  WHERE e.status IN (0, 1, 3)
			    AND (e.next_retry_time IS NULL OR e.next_retry_time <= #{now})
			    AND (e.locked_at IS NULL OR e.locked_at < DATE_SUB(#{now}, INTERVAL #{lockTimeout} SECOND))
			) t
			WHERE t.rn = 1
			ORDER BY t.id
			LIMIT #{limit}
			""")
	List<OutboxEvent> selectPendingEvents(@Param("now") Instant now, @Param("lockTimeout") int lockTimeout,
			@Param("limit") int limit);

	/**
	 * 抢占锁定事件
	 * @param ids 事件ID列表
	 * @param lockedBy 锁定者标识
	 * @param lockedAt 锁定时间
	 * @return 成功锁定数量
	 */
	@Update("""
			<script>
			UPDATE outbox_event
			SET status = 1, locked_by = #{lockedBy}, locked_at = #{lockedAt}
			WHERE id IN
			<foreach collection="ids" item="id" open="(" separator="," close=")">
			  #{id}
			</foreach>
			  AND status IN (0, 1, 3)
			  AND (locked_at IS NULL OR locked_at &lt; DATE_SUB(#{lockedAt}, INTERVAL 60 SECOND))
			</script>
			""")
	int claimEvents(@Param("ids") List<Long> ids, @Param("lockedBy") String lockedBy,
			@Param("lockedAt") Instant lockedAt);

}
