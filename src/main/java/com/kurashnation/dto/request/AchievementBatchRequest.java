package com.kurashnation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AchievementBatchRequest(
        @NotNull Long athleteId,
        @NotEmpty @Valid List<AchievementItemRequest> items
) {
}
