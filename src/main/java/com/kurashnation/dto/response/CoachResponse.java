package com.kurashnation.dto.response;

public record CoachResponse(
        Long id,
        UserResponse user,
        String region,
        boolean federationMember,
        Integer experienceYears
) {
}

