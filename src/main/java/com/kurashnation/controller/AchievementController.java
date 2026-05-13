package com.kurashnation.controller;

import com.kurashnation.dto.request.AchievementBatchRequest;
import com.kurashnation.dto.request.AchievementRequest;
import com.kurashnation.dto.request.CohortInsightRequest;
import com.kurashnation.config.AiIntegrationProperties;
import com.kurashnation.dto.response.AchievementInsightResponse;
import com.kurashnation.dto.response.AchievementResponse;
import com.kurashnation.dto.response.AiSettingsStatusResponse;
import com.kurashnation.dto.response.CohortInsightResponse;
import com.kurashnation.service.interfaces.AchievementInsightService;
import com.kurashnation.service.interfaces.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/achievements")
public class AchievementController {

    private final AchievementService achievementService;
    private final AchievementInsightService achievementInsightService;
    private final AiIntegrationProperties aiIntegrationProperties;

    public AchievementController(
            AchievementService achievementService,
            AchievementInsightService achievementInsightService,
            AiIntegrationProperties aiIntegrationProperties
    ) {
        this.achievementService = achievementService;
        this.achievementInsightService = achievementInsightService;
        this.aiIntegrationProperties = aiIntegrationProperties;
    }

    @Operation(summary = "LLM integration status (no secrets exposed)")
    @ApiResponse(responseCode = "200", description = "Whether API key is set server-side; base URL and model hints")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/ai/status")
    public AiSettingsStatusResponse aiStatus() {
        return new AiSettingsStatusResponse(
                aiIntegrationProperties.isConfigured(),
                aiIntegrationProperties.baseUrl(),
                aiIntegrationProperties.model()
        );
    }

    @Operation(summary = "My achievements")
    @ApiResponse(responseCode = "200", description = "Achievements list")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me")
    public List<AchievementResponse> my(Authentication authentication) {
        return achievementService.myAchievements(authentication.getName());
    }

    @Operation(summary = "Request achievement add (athlete)")
    @ApiResponse(responseCode = "200", description = "Achievement created (unverified)")
    @PreAuthorize("hasRole('ATHLETE')")
    @PostMapping("/request")
    public AchievementResponse request(Authentication authentication, @Valid @RequestBody AchievementRequest request) {
        return achievementService.requestAddAchievement(authentication.getName(), request);
    }

    @Operation(summary = "Request several achievements with weekly schedules (athlete)")
    @ApiResponse(responseCode = "200", description = "Achievements created")
    @PreAuthorize("hasRole('ATHLETE')")
    @PostMapping("/request/batch")
    public List<AchievementResponse> requestBatch(
            Authentication authentication,
            @Valid @RequestBody AchievementBatchRequest request
    ) {
        return achievementService.requestBatchAchievements(authentication.getName(), request);
    }

    @Operation(summary = "AI-style insight from achievements and weekly patterns (athlete)")
    @ApiResponse(responseCode = "200", description = "Insight and suggested week plan")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me/insight")
    public AchievementInsightResponse myInsight(Authentication authentication) {
        return achievementInsightService.insightForAthlete(authentication.getName());
    }

    @Operation(summary = "Cohort insight for coach's athletes")
    @ApiResponse(responseCode = "200", description = "Aggregated insight")
    @PreAuthorize("hasRole('COACH')")
    @PostMapping("/coach/cohort-insight")
    public CohortInsightResponse cohortInsight(
            Authentication authentication,
            @RequestBody(required = false) CohortInsightRequest request
    ) {
        CohortInsightRequest body = request == null ? new CohortInsightRequest(null) : request;
        return achievementInsightService.insightForCoach(authentication.getName(), body);
    }

    @Operation(summary = "Update achievement (admin/coach)")
    @ApiResponse(responseCode = "200", description = "Achievement updated")
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    @PutMapping("/{id}")
    public AchievementResponse update(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody AchievementRequest request
    ) {
        return achievementService.updateAchievement(authentication.getName(), id, request);
    }

    @Operation(summary = "Delete achievement (admin)")
    @ApiResponse(responseCode = "200", description = "Achievement deleted")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable("id") Long id) {
        achievementService.deleteAchievement(authentication.getName(), id);
    }
}

