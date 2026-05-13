package com.kurashnation.controller;

import com.kurashnation.dto.response.RecommendationResponse;
import com.kurashnation.service.interfaces.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "My latest recommendation")
    @ApiResponse(responseCode = "200", description = "Recommendation")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me/latest")
    public ResponseEntity<RecommendationResponse> latest(Authentication authentication) {
        return recommendationService.latestForMe(authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @Operation(summary = "Generate and save recommendation for current athlete")
    @ApiResponse(responseCode = "200", description = "Recommendation generated")
    @PreAuthorize("hasRole('ATHLETE')")
    @PostMapping("/me/generate")
    public RecommendationResponse generateMine(Authentication authentication) {
        return recommendationService.generateForMe(authentication.getName());
    }

    @Operation(summary = "Generate recommendation for athlete (coach/admin)")
    @ApiResponse(responseCode = "200", description = "Generated recommendation")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @PostMapping("/generate/{athleteId}")
    public RecommendationResponse generate(Authentication authentication, @PathVariable("athleteId") Long athleteId) {
        return recommendationService.generateForAthlete(authentication.getName(), athleteId);
    }

    @Operation(summary = "My week plan")
    @ApiResponse(responseCode = "200", description = "Week plan JSON")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me/week-plan")
    public Map<String, String> weekPlan(Authentication authentication) {
        return recommendationService.weekPlanForMe(authentication.getName());
    }

    @Operation(summary = "Compare with gold standard")
    @ApiResponse(responseCode = "200", description = "Comparison")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me/compare")
    public Map<String, Object> compare(Authentication authentication) {
        return recommendationService.compareWithGoldStandard(authentication.getName());
    }

    @Operation(summary = "Export my recommendation to PDF")
    @ApiResponse(responseCode = "200", description = "PDF file")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me/export-pdf")
    public ResponseEntity<byte[]> export(Authentication authentication) {
        byte[] pdf = recommendationService.exportPdfForMe(authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recommendation.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

