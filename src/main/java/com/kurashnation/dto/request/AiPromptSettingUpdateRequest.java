package com.kurashnation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPromptSettingUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String label,
        @NotBlank
        String body
) {
}
