package com.kurashnation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RegisterAthleteRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName,
        String phone,
        @NotBlank String region,
        @NotBlank String weightCategory,
        @NotNull LocalDate dateOfBirth,
        String rank,
        Long coachId
) {
}

