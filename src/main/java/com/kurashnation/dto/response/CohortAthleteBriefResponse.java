package com.kurashnation.dto.response;

public record CohortAthleteBriefResponse(
        Long athleteId,
        String fullName,
        int achievementCount,
        int trainingWeekCount
) {
}
