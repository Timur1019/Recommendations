package com.kurashnation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterCoachRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100)
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 72, message = "Password must be 6–72 characters")
        String password,
        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,
        @Size(max = 20)
        String phone,
        @Size(max = 50)
        String region,
        Boolean federationMember,
        @Min(value = 0, message = "Experience years must be >= 0")
        @Max(value = 80, message = "Experience years must be <= 80")
        Integer experienceYears
) {
}

