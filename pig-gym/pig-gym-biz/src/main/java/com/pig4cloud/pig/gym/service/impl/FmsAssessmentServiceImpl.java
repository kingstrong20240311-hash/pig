package com.pig4cloud.pig.gym.service.impl;

import com.pig4cloud.pig.gym.api.dto.CreateFmsAssessmentRequest;
import com.pig4cloud.pig.gym.api.dto.FmsAssessmentItemInput;
import com.pig4cloud.pig.gym.api.dto.FmsAssessmentResult;
import com.pig4cloud.pig.gym.api.entity.FmsAssessment;
import com.pig4cloud.pig.gym.api.entity.FmsAssessmentItem;
import com.pig4cloud.pig.gym.api.enums.FmsMovementType;
import com.pig4cloud.pig.gym.api.enums.FmsVersionType;
import com.pig4cloud.pig.gym.mapper.FmsAssessmentItemMapper;
import com.pig4cloud.pig.gym.mapper.FmsAssessmentMapper;
import com.pig4cloud.pig.gym.service.FmsAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * FMS评估服务实现
 */
@Service
@RequiredArgsConstructor
public class FmsAssessmentServiceImpl implements FmsAssessmentService {

	private static final Set<FmsMovementType> CLEARING_TEST_MOVEMENTS = EnumSet.of(FmsMovementType.SHOULDER_MOBILITY,
			FmsMovementType.TRUNK_STABILITY_PUSHUP, FmsMovementType.ROTARY_STABILITY);

	private static final Set<FmsMovementType> BILATERAL_MOVEMENTS = EnumSet.of(FmsMovementType.HURDLE_STEP,
			FmsMovementType.INLINE_LUNGE, FmsMovementType.SHOULDER_MOBILITY,
			FmsMovementType.ACTIVE_STRAIGHT_LEG_RAISE, FmsMovementType.ROTARY_STABILITY);

	private final FmsAssessmentMapper fmsAssessmentMapper;

	private final FmsAssessmentItemMapper fmsAssessmentItemMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public FmsAssessmentResult createAssessment(CreateFmsAssessmentRequest request) {
		validateRequest(request);

		FmsAssessment assessment = new FmsAssessment();
		assessment.setMemberId(request.getMemberId());
		assessment.setCoachId(request.getCoachId());
		assessment.setAssessmentType(request.getAssessmentType());
		assessment.setVersionType(request.getVersionType());
		assessment.setTrainingSuggestion(request.getTrainingSuggestion());

		fmsAssessmentMapper.insert(assessment);

		int totalScore = 0;
		int restrictedMovementCount = 0;
		boolean hasAsymmetry = false;
		boolean hasPainRisk = false;

		for (FmsAssessmentItemInput itemInput : request.getItems()) {
			FmsAssessmentItem item = buildAndValidateItem(assessment.getId(), itemInput);
			fmsAssessmentItemMapper.insert(item);

			totalScore += item.getFinalScore();
			if (item.getFinalScore() <= 2) {
				restrictedMovementCount++;
			}
			if (item.getLeftScore() != null && item.getRightScore() != null && !item.getLeftScore().equals(item.getRightScore())) {
				hasAsymmetry = true;
			}
			if ((item.getFinalScore() == 0) || Boolean.TRUE.equals(item.getClearingTestPain())) {
				hasPainRisk = true;
			}
		}

		assessment.setTotalScore(totalScore);
		assessment.setRestrictedMovementCount(restrictedMovementCount);
		assessment.setHasAsymmetry(hasAsymmetry);
		assessment.setHasPainRisk(hasPainRisk);
		fmsAssessmentMapper.updateById(assessment);

		FmsAssessmentResult result = new FmsAssessmentResult();
		result.setAssessmentId(assessment.getId());
		result.setTotalScore(totalScore);
		result.setRestrictedMovementCount(restrictedMovementCount);
		result.setHasAsymmetry(hasAsymmetry);
		result.setHasPainRisk(hasPainRisk);
		return result;
	}

	private void validateRequest(CreateFmsAssessmentRequest request) {
		if (request.getVersionType() == FmsVersionType.OFFICIAL_7) {
			Set<FmsMovementType> movementSet = EnumSet.noneOf(FmsMovementType.class);
			for (FmsAssessmentItemInput item : request.getItems()) {
				if (!movementSet.add(item.getMovementType())) {
					throw new IllegalArgumentException("官方7项评估动作不允许重复: " + item.getMovementType());
				}
			}
			if (movementSet.size() != FmsMovementType.values().length) {
				throw new IllegalArgumentException("官方7项评估必须包含全部7个动作");
			}
		}
	}

	private FmsAssessmentItem buildAndValidateItem(Long assessmentId, FmsAssessmentItemInput input) {
		FmsAssessmentItem item = new FmsAssessmentItem();
		item.setAssessmentId(assessmentId);
		item.setMovementType(input.getMovementType());
		item.setHasClearingTest(Boolean.TRUE.equals(input.getHasClearingTest()));
		item.setClearingTestPain(Boolean.TRUE.equals(input.getClearingTestPain()));
		item.setPainPosition(input.getPainPosition());
		item.setCompensationTags(input.getCompensationTags());
		item.setRemark(input.getRemark());

		if (Boolean.TRUE.equals(item.getHasClearingTest()) && !CLEARING_TEST_MOVEMENTS.contains(input.getMovementType())) {
			throw new IllegalArgumentException("当前动作不支持清除测试: " + input.getMovementType());
		}
		if (Boolean.TRUE.equals(item.getClearingTestPain()) && !Boolean.TRUE.equals(item.getHasClearingTest())) {
			throw new IllegalArgumentException("清除测试疼痛必须在 hasClearingTest=true 时设置");
		}

		Integer finalScore;
		if (BILATERAL_MOVEMENTS.contains(input.getMovementType())) {
			if (input.getLeftScore() == null || input.getRightScore() == null) {
				throw new IllegalArgumentException("双侧动作必须同时填写左右评分: " + input.getMovementType());
			}
			validateScore(input.getLeftScore(), "leftScore");
			validateScore(input.getRightScore(), "rightScore");
			item.setLeftScore(input.getLeftScore());
			item.setRightScore(input.getRightScore());
			finalScore = Math.min(input.getLeftScore(), input.getRightScore());
		}
		else {
			if (input.getFinalScore() == null) {
				throw new IllegalArgumentException("非双侧动作必须填写 finalScore: " + input.getMovementType());
			}
			validateScore(input.getFinalScore(), "finalScore");
			finalScore = input.getFinalScore();
		}

		if (Boolean.TRUE.equals(item.getClearingTestPain())) {
			if (item.getPainPosition() == null || item.getPainPosition().isBlank()) {
				throw new IllegalArgumentException("清除测试疼痛时必须填写 painPosition");
			}
			finalScore = 0;
		}

		item.setFinalScore(finalScore);
		return item;
	}

	private void validateScore(Integer score, String fieldName) {
		if (score == null || score < 0 || score > 3) {
			throw new IllegalArgumentException(fieldName + " 仅允许 0~3");
		}
	}

}
