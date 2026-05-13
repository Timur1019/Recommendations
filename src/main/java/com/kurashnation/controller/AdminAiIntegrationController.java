package com.kurashnation.controller;

import com.kurashnation.config.AiIntegrationProperties;
import com.kurashnation.dto.response.AiSettingsStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiIntegrationController {

    private final AiIntegrationProperties aiIntegrationProperties;

    public AdminAiIntegrationController(AiIntegrationProperties aiIntegrationProperties) {
        this.aiIntegrationProperties = aiIntegrationProperties;
    }

    @Operation(summary = "LLM integration status for admin (no secrets)")
    @ApiResponse(responseCode = "200", description = "Configured flag, base URL, model")
    @GetMapping("/integration")
    public AiSettingsStatusResponse integration() {
        return new AiSettingsStatusResponse(
                aiIntegrationProperties.isConfigured(),
                aiIntegrationProperties.baseUrl(),
                aiIntegrationProperties.model()
        );
    }
}
