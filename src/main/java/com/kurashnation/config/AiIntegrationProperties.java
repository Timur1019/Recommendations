package com.kurashnation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External LLM (OpenAI Responses API). Set via {@code application.yml} or env {@code AI_API_KEY} or
 * {@code OPENAI_API_KEY}, optional {@code AI_BASE_URL}, {@code AI_MODEL}. Keys must stay on the server.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiIntegrationProperties(
        String apiKey,
        String baseUrl,
        String model
) {
    public AiIntegrationProperties {
        if (apiKey != null) {
            apiKey = apiKey.strip();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        } else {
            baseUrl = baseUrl.strip();
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        } else {
            model = model.strip();
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
