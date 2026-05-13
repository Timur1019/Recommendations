package com.kurashnation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertAthleteRequest(
        @NotBlank String fullName,
        String phone,
        @NotBlank String region,
        @NotBlank String weightCategory,
        @NotNull LocalDate dateOfBirth,
        String rank,
        Long coachId,
        Integer currentMedalCountGold,
        Integer currentMedalCountSilver,
        Integer currentMedalCountBronze,
        @Size(max = 80) String sportType,
        @Min(120) @Max(250) Integer heightCm,
        @DecimalMin("30.0") @DecimalMax("250.0") BigDecimal bodyWeightKg,
        @Size(max = 4000) String goalText
) {
}

