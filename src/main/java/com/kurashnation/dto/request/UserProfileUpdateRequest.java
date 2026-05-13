package com.kurashnation.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Partial update: {@code null} field means "leave unchanged"; empty string clears optional text fields.
 */
public record UserProfileUpdateRequest(
        @Size(max = 500) String avatarUrl,
        @Size(max = 20) String phone
) {
}
