package com.kurashnation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TrainingLibraryApplyHandbookRequest(
        @NotNull Long athleteId,
        @NotBlank String summary,
        @NotNull Map<String, String> weekPlan
) {
}
