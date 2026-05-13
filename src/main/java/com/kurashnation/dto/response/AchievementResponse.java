package com.kurashnation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AchievementResponse(
        Long id,
        Long athleteId,
        String competitionName,
        LocalDate competitionDate,
        String competitionLevel,
        String medalType,
        String medalPhotoUrl,
        boolean verifiedByAdmin,
        List<AchievementWeekResponse> trainingWeeks,
        List<AchievementMediaResponse> media
) {
}

