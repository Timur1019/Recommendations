package com.kurashnation.dto.response;

public record AiSettingsStatusResponse(
        boolean llmConfigured,
        String baseUrl,
        String model
) {
}
