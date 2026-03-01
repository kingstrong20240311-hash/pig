package com.pig4cloud.pig.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.gym.api.dto.CompleteTrainingSessionRequest;
import com.pig4cloud.pig.gym.api.dto.CreateTrainingSessionRequest;
import com.pig4cloud.pig.gym.api.entity.TrainingSession;
import com.pig4cloud.pig.gym.api.vo.DailySessionVO;

import java.util.List;

/**
 * 训练课程服务
 */
public interface TrainingSessionService extends IService<TrainingSession> {

	TrainingSession createSession(CreateTrainingSessionRequest request);

	TrainingSession cancelSession(Long sessionId, Long coachId, String cancelReason);

	TrainingSession completeSession(CompleteTrainingSessionRequest request);

	List<DailySessionVO> getDailySchedule(Long coachId, Long startMs, Long endMs);

}
