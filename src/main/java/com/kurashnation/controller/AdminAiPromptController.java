package com.kurashnation.controller;

import com.kurashnation.dto.request.AiPromptSettingUpdateRequest;
import com.kurashnation.dto.response.AiPromptSettingResponse;
import com.kurashnation.service.interfaces.AiPromptSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/ai/prompts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiPromptController {

    private final AiPromptSettingService aiPromptSettingService;

    public AdminAiPromptController(AiPromptSettingService aiPromptSettingService) {
        this.aiPromptSettingService = aiPromptSettingService;
    }

    @Operation(summary = "List editable AI prompt templates")
    @ApiResponse(responseCode = "200", description = "Prompt list")
    @GetMapping
    public List<AiPromptSettingResponse> list() {
        return aiPromptSettingService.listAll();
    }

    @Operation(summary = "Update AI prompt template by key")
    @ApiResponse(responseCode = "200", description = "Updated")
    @PutMapping("/{settingKey}")
    public AiPromptSettingResponse update(
            @PathVariable("settingKey") String settingKey,
            @RequestBody @Valid AiPromptSettingUpdateRequest request
    ) {
        return aiPromptSettingService.update(settingKey, request);
    }
}
