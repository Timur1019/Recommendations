package com.kurashnation.controller;

import com.kurashnation.dto.request.TrainingRequest;
import com.kurashnation.dto.response.TrainingResponse;
import com.kurashnation.service.interfaces.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trainings")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Operation(summary = "My trainings")
    @ApiResponse(responseCode = "200", description = "Trainings list")
    @PreAuthorize("hasAnyRole('ATHLETE','COACH')")
    @GetMapping("/me")
    public List<TrainingResponse> myTrainings(Authentication authentication) {
        return trainingService.getMyTrainings(authentication.getName());
    }

    @Operation(summary = "Add training")
    @ApiResponse(responseCode = "200", description = "Training created")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @PostMapping
    public TrainingResponse create(Authentication authentication, @Valid @RequestBody TrainingRequest request) {
        return trainingService.createTraining(authentication.getName(), request);
    }

    @Operation(summary = "Update training")
    @ApiResponse(responseCode = "200", description = "Training updated")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @PutMapping("/{id}")
    public TrainingResponse update(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody TrainingRequest request
    ) {
        return trainingService.updateTraining(authentication.getName(), id, request);
    }

    @Operation(summary = "Delete training")
    @ApiResponse(responseCode = "200", description = "Training deleted")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable("id") Long id) {
        trainingService.deleteTraining(authentication.getName(), id);
    }

    @Operation(summary = "Weekly trainings statistics")
    @ApiResponse(responseCode = "200", description = "Weekly stats")
    @PreAuthorize("hasRole('ATHLETE')")
    @GetMapping("/statistics/weekly")
    public Map<String, Object> weekly(Authentication authentication) {
        return trainingService.weeklyStatistics(authentication.getName());
    }
}

