package com.kurashnation.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;

public record TrainingResponse(
        Long id,
        Long athleteId,
        LocalDate trainingDate,
        String workoutType,
        int durationMinutes,
        Integer intensity,
        JsonNode technicalActions,
        String notes,
        Long createdByUserId
) {
}

