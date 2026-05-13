package com.kurashnation.controller;

import com.kurashnation.dto.request.TrainingLibraryAnalyzeRequest;
import com.kurashnation.dto.request.TrainingLibraryApplyHandbookRequest;
import com.kurashnation.dto.response.HandbookWeekPlanLlm;
import com.kurashnation.dto.response.MediaDownload;
import com.kurashnation.dto.response.RecommendationResponse;
import com.kurashnation.dto.response.TrainingLibraryFileResponse;
import com.kurashnation.dto.response.TrainingLibraryPreviewTextResponse;
import com.kurashnation.service.interfaces.TrainingLibraryHandbookService;
import com.kurashnation.service.interfaces.TrainingLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/training-library")
public class TrainingLibraryController {

    private final TrainingLibraryService trainingLibraryService;
    private final TrainingLibraryHandbookService trainingLibraryHandbookService;

    public TrainingLibraryController(
            TrainingLibraryService trainingLibraryService,
            TrainingLibraryHandbookService trainingLibraryHandbookService
    ) {
        this.trainingLibraryService = trainingLibraryService;
        this.trainingLibraryHandbookService = trainingLibraryHandbookService;
    }

    @Operation(summary = "List training library files (coach)")
    @ApiResponse(responseCode = "200", description = "Files list")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @GetMapping
    public List<TrainingLibraryFileResponse> list() {
        return trainingLibraryService.listForCoach();
    }

    @Operation(summary = "Extracted text preview (coach) — Word/Excel/PDF")
    @ApiResponse(responseCode = "200", description = "Plain text for in-app reading")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @GetMapping("/files/{id}/preview-text")
    public TrainingLibraryPreviewTextResponse previewText(@PathVariable("id") Long id) {
        return trainingLibraryHandbookService.previewText(id);
    }

    @Operation(summary = "AI: week plan from handbook document")
    @ApiResponse(responseCode = "200", description = "Summary and weekPlan")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @PostMapping("/files/{id}/analyze")
    public HandbookWeekPlanLlm analyze(
            Authentication authentication,
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid TrainingLibraryAnalyzeRequest body
    ) {
        return trainingLibraryHandbookService.analyze(authentication.getName(), id, body);
    }

    @Operation(summary = "Save AI handbook plan as athlete recommendation")
    @ApiResponse(responseCode = "200", description = "Saved recommendation")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @PostMapping("/files/{id}/apply-handbook")
    public RecommendationResponse applyHandbook(
            Authentication authentication,
            @PathVariable("id") Long id,
            @RequestBody @Valid TrainingLibraryApplyHandbookRequest body
    ) {
        return trainingLibraryHandbookService.applyHandbook(authentication.getName(), id, body);
    }

    @Operation(summary = "Inline view or download file (coach)")
    @ApiResponse(responseCode = "200", description = "Binary content")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> file(
            @PathVariable("id") Long id,
            @RequestParam(value = "download", required = false, defaultValue = "0") int download
    ) {
        MediaDownload d = trainingLibraryService.prepareDownload(id);
        boolean asAttachment = download == 1;
        ContentDisposition.Builder dispositionBuilder = asAttachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline();
        ContentDisposition finalDisposition = dispositionBuilder
                .filename(d.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, finalDisposition.toString())
                .contentType(MediaType.parseMediaType(d.contentType()))
                .body(d.resource());
    }
}

