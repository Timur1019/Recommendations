package com.kurashnation.dto.response;

import java.time.Instant;

public record TrainingRequestRowResponse(
        Long id,
        String status,
        Instant createdAt,
        String note,
        Long athleteId,
        String athleteFullName,
        Long coachId,
        String coachFullName
) {
}
