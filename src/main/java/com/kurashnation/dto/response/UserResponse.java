package com.kurashnation.dto.response;

public record UserResponse(
        Long id,
        String fullName,
        String role,
        String email,
        String phone,
        String avatarUrl,
        boolean active,
        Long athleteId,
        Long coachId
) {
}
