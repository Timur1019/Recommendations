package com.kurashnation.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.kurashnation.model.enums.WorkoutType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TrainingRequest(
        @NotNull Long athleteId,
        @NotNull LocalDate trainingDate,
        @NotNull WorkoutType workoutType,
        @Min(1) int durationMinutes,
        @Min(1) @Max(10) Integer intensity,
        JsonNode technicalActions,
        String notes
) {
}

