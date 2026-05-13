package com.kurashnation.dto.request;

import com.kurashnation.model.enums.CompetitionLevel;
import com.kurashnation.model.enums.MedalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AchievementRequest(
        @NotNull Long athleteId,
        @NotBlank String competitionName,
        @NotNull LocalDate competitionDate,
        @NotNull CompetitionLevel competitionLevel,
        @NotNull MedalType medalType,
        String medalPhotoUrl,
        @Valid List<AchievementWeekRequest> weeks
) {
}

