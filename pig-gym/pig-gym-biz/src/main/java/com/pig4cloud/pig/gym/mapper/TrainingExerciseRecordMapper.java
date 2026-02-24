package com.pig4cloud.pig.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.gym.api.entity.TrainingExerciseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 训练动作记录 Mapper
 */
@Mapper
public interface TrainingExerciseRecordMapper extends BaseMapper<TrainingExerciseRecord> {

}
