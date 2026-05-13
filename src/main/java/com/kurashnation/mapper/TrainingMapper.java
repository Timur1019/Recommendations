package com.kurashnation.mapper;

import com.kurashnation.dto.response.TrainingResponse;
import com.kurashnation.model.entity.Training;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrainingMapper extends BaseMapper<Training, TrainingResponse> {
    @Override
    @Mapping(target = "athleteId", expression = "java(training.getAthlete().getId())")
    @Mapping(target = "workoutType", expression = "java(training.getWorkoutType().name())")
    @Mapping(target = "createdByUserId", expression = "java(training.getCreatedBy() == null ? null : training.getCreatedBy().getId())")
    TrainingResponse toDto(Training training);
}

