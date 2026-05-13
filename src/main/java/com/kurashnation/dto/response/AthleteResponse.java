package com.kurashnation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AthleteResponse(
        Long id,
        UserResponse user,
        Long coachId,
        String region,
        String weightCategory,
        LocalDate dateOfBirth,
        String rank,
        String sportType,
        Integer heightCm,
        BigDecimal bodyWeightKg,
        String goalText,
        int currentMedalCountGold,
        int currentMedalCountSilver,
        int currentMedalCountBronze
) {
}

