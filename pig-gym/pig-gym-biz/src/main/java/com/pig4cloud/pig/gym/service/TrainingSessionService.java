package com.pig4cloud.pig.gym.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.gym.api.dto.CompleteTrainingSessionRequest;
import com.pig4cloud.pig.gym.api.dto.CreateTrainingSessionRequest;
import com.pig4cloud.pig.gym.api.entity.TrainingSession;

/**
 * 训练课程服务
 */
public interface TrainingSessionService extends IService<TrainingSession> {

	TrainingSession createSession(CreateTrainingSessionRequest request);

	TrainingSession cancelSession(Long sessionId, Long coachId, String cancelReason);

	TrainingSession completeSession(CompleteTrainingSessionRequest request);

}
