package com.kurashnation.controller;

import com.kurashnation.dto.request.SubmitTrainingRequestNoteDto;
import com.kurashnation.dto.response.TrainingRequestRowResponse;
import com.kurashnation.service.interfaces.TrainingRequestListService;
import com.kurashnation.service.interfaces.TrainingRequestSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/training-requests")
public class TrainingRequestSubmissionController {

    private final TrainingRequestSubmissionService trainingRequestSubmissionService;
    private final TrainingRequestListService trainingRequestListService;

    public TrainingRequestSubmissionController(
            TrainingRequestSubmissionService trainingRequestSubmissionService,
            TrainingRequestListService trainingRequestListService
    ) {
        this.trainingRequestSubmissionService = trainingRequestSubmissionService;
        this.trainingRequestListService = trainingRequestListService;
    }

    @Operation(summary = "My submitted training requests (athlete)")
    @ApiResponse(responseCode = "200", description = "List")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/me")
    public List<TrainingRequestRowResponse> listMine(Authentication authentication) {
        return trainingRequestListService.listMine(authentication.getName());
    }

    @Operation(summary = "Training requests from my athletes (coach)")
    @ApiResponse(responseCode = "200", description = "List")
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/coach")
    public List<TrainingRequestRowResponse> listForCoach(Authentication authentication) {
        return trainingRequestListService.listForCoach(authentication.getName());
    }

    @Operation(summary = "All training requests (admin)")
    @ApiResponse(responseCode = "200", description = "List")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public List<TrainingRequestRowResponse> listForAdmin(Authentication authentication) {
        return trainingRequestListService.listForAdmin(authentication.getName());
    }

    @Operation(summary = "Athlete submits a request to add a training (for coach review)")
    @ApiResponse(responseCode = "201", description = "Request created")
    @PreAuthorize("hasRole('ATHLETE')")
    @PostMapping("/me")
    public ResponseEntity<Map<String, Long>> submit(
            Authentication authentication,
            @Valid @RequestBody(required = false) SubmitTrainingRequestNoteDto dto
    ) {
        long id = trainingRequestSubmissionService.submitForAthlete(authentication.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }
}
