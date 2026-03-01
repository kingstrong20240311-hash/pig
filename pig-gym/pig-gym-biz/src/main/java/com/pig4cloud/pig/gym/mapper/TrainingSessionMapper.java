package com.pig4cloud.pig.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.gym.api.entity.TrainingSession;
import com.pig4cloud.pig.gym.api.vo.DailySessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.InstantTypeHandler;

import java.time.Instant;
import java.util.List;

/**
 * 训练课程 Mapper
 */
@Mapper
public interface TrainingSessionMapper extends BaseMapper<TrainingSession> {

	@Select("""
			SELECT
			    ts.id,
			    ts.member_id,
			    m.name         AS member_name,
			    m.avatar_url   AS member_avatar,
			    lp.training_goal,
			    ts.scheduled_at,
			    ts.status
			FROM gym_training_session ts
			JOIN gym_member m ON ts.member_id = m.id AND m.del_flag = '0'
			LEFT JOIN gym_lesson_plan lp ON ts.lesson_plan_id = lp.id AND lp.del_flag = '0'
			WHERE ts.coach_id = #{coachId}
			  AND ts.scheduled_at >= #{start}
			  AND ts.scheduled_at < #{end}
			  AND ts.del_flag = '0'
			ORDER BY ts.scheduled_at ASC
			""")
	@Results({ @Result(column = "id", property = "id"), @Result(column = "member_id", property = "memberId"),
			@Result(column = "member_name", property = "memberName"),
			@Result(column = "member_avatar", property = "memberAvatar"),
			@Result(column = "training_goal", property = "trainingGoal"),
			@Result(column = "scheduled_at", property = "scheduledAt", typeHandler = InstantTypeHandler.class),
			@Result(column = "status", property = "status") })
	List<DailySessionVO> selectDailySchedule(@Param("coachId") Long coachId, @Param("start") Instant start,
			@Param("end") Instant end);

}
