package com.pig4cloud.pig.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.gym.api.entity.TrainingExerciseMedia;
import com.pig4cloud.pig.gym.mapper.TrainingExerciseMediaMapper;
import com.pig4cloud.pig.gym.service.TrainingExerciseMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 训练动作素材服务实现
 */
@Service
@RequiredArgsConstructor
public class TrainingExerciseMediaServiceImpl extends ServiceImpl<TrainingExerciseMediaMapper, TrainingExerciseMedia>
		implements TrainingExerciseMediaService {

}
