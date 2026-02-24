package com.pig4cloud.pig.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.gym.api.dto.CompleteTrainingSessionRequest;
import com.pig4cloud.pig.gym.api.dto.CreateTrainingSessionRequest;
import com.pig4cloud.pig.gym.api.dto.TrainingExerciseRecordInput;
import com.pig4cloud.pig.gym.api.entity.TrainingExerciseRecord;
import com.pig4cloud.pig.gym.api.entity.TrainingSession;
import com.pig4cloud.pig.gym.api.enums.SessionStatus;
import com.pig4cloud.pig.gym.mapper.TrainingExerciseRecordMapper;
import com.pig4cloud.pig.gym.mapper.TrainingSessionMapper;
import com.pig4cloud.pig.gym.service.TrainingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 训练课程服务实现
 */
@Service
@RequiredArgsConstructor
public class TrainingSessionServiceImpl extends ServiceImpl<TrainingSessionMapper, TrainingSession>
		implements TrainingSessionService {

	private final TrainingExerciseRecordMapper trainingExerciseRecordMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrainingSession createSession(CreateTrainingSessionRequest request) {
		TrainingSession session = new TrainingSession();
		session.setMemberId(request.getMemberId());
		session.setCoachId(request.getCoachId());
		session.setLessonPlanId(request.getLessonPlanId());
		session.setScheduledAt(request.getScheduledAt());
		session.setStatus(SessionStatus.SCHEDULED);
		baseMapper.insert(session);
		return session;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrainingSession cancelSession(Long sessionId, Long coachId, String cancelReason) {
		TrainingSession session = getExistingSession(sessionId);
		if (!session.getCoachId().equals(coachId)) {
			throw new IllegalArgumentException("仅课程教练可以取消预约");
		}
		if (session.getStatus() != SessionStatus.SCHEDULED) {
			throw new IllegalArgumentException("仅已预约课程允许取消");
		}
		session.setStatus(SessionStatus.CANCELED);
		session.setCancelReason(cancelReason);
		baseMapper.updateById(session);
		return session;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrainingSession completeSession(CompleteTrainingSessionRequest request) {
		TrainingSession session = getExistingSession(request.getSessionId());
		if (!session.getCoachId().equals(request.getCoachId())) {
			throw new IllegalArgumentException("仅课程教练可以结课");
		}
		if (session.getStatus() != SessionStatus.SCHEDULED) {
			throw new IllegalArgumentException("课程状态不允许结课");
		}

		for (TrainingExerciseRecordInput input : request.getExerciseRecords()) {
			validateExerciseRecord(input);
			TrainingExerciseRecord record = new TrainingExerciseRecord();
			record.setSessionId(session.getId());
			record.setExerciseName(input.getExerciseName());
			record.setWeightKg(input.getWeightKg());
			record.setReps(input.getReps());
			record.setSets(input.getSets());
			record.setSortOrder(input.getSortOrder());
			trainingExerciseRecordMapper.insert(record);
		}

		session.setStatus(SessionStatus.COMPLETED);
		session.setCompletedAt(request.getCompletedAt() == null ? LocalDateTime.now() : request.getCompletedAt());
		baseMapper.updateById(session);
		return session;
	}

	private TrainingSession getExistingSession(Long sessionId) {
		TrainingSession session = baseMapper.selectById(sessionId);
		if (session == null) {
			throw new IllegalArgumentException("课程不存在: " + sessionId);
		}
		return session;
	}

	private void validateExerciseRecord(TrainingExerciseRecordInput input) {
		if (input.getWeightKg() == null || input.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("动作重量必须大于0");
		}
		if (input.getReps() == null || input.getReps() <= 0) {
			throw new IllegalArgumentException("动作次数必须大于0");
		}
		if (input.getSets() == null || input.getSets() <= 0) {
			throw new IllegalArgumentException("动作组数必须大于0");
		}
	}

}
