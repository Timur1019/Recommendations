package com.kurashnation.dto.response;

import java.time.Instant;

public record AiPromptSettingResponse(
        String settingKey,
        String label,
        String body,
        Instant updatedAt
) {
}
