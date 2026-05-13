package com.kurashnation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AthleteNameRequest(
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
