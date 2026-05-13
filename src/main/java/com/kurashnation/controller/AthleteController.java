package com.kurashnation.controller;

import com.kurashnation.dto.request.AthleteNameRequest;
import com.kurashnation.dto.request.UpsertAthleteRequest;
import com.kurashnation.dto.response.AthleteResponse;
import com.kurashnation.service.interfaces.AthleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/athletes")
public class AthleteController {

    private final AthleteService athleteService;

    public AthleteController(AthleteService athleteService) {
        this.athleteService = athleteService;
    }

    @Operation(summary = "My athlete profile")
    @ApiResponse(responseCode = "200", description = "Athlete profile")
    @PreAuthorize("hasAnyRole('ATHLETE','COACH','ADMIN')")
    @GetMapping("/me")
    public AthleteResponse me(Authentication authentication) {
        return athleteService.getMyProfile(authentication.getName());
    }

    @Operation(summary = "Update my athlete profile")
    @ApiResponse(responseCode = "200", description = "Updated profile")
    @PreAuthorize("hasRole('ATHLETE')")
    @PutMapping("/me")
    public AthleteResponse updateMe(Authentication authentication, @Valid @RequestBody UpsertAthleteRequest request) {
        return athleteService.updateMyProfile(authentication.getName(), request);
    }

    @Operation(summary = "Update my first and last name (display name)")
    @ApiResponse(responseCode = "200", description = "Name updated")
    @PreAuthorize("hasRole('ATHLETE')")
    @PatchMapping("/me/name")
    public AthleteResponse patchMyName(Authentication authentication, @Valid @RequestBody AthleteNameRequest request) {
        return athleteService.patchMyDisplayName(authentication.getName(), request);
    }

    @Operation(summary = "Get athlete profile by id (access controlled)")
    @ApiResponse(responseCode = "200", description = "Athlete profile")
    @PreAuthorize("hasAnyRole('ATHLETE','COACH','ADMIN')")
    @GetMapping("/{id}")
    public AthleteResponse getById(Authentication authentication, @PathVariable("id") Long id) {
        return athleteService.getAthleteById(authentication.getName(), id);
    }

    @Operation(summary = "List athletes of coach")
    @ApiResponse(responseCode = "200", description = "Coach athletes")
    @PreAuthorize("hasAnyRole('COACH','ADMIN')")
    @GetMapping("/coach/{coachId}")
    public List<AthleteResponse> coachAthletes(Authentication authentication, @PathVariable("coachId") Long coachId) {
        return athleteService.getAthletesByCoach(authentication.getName(), coachId);
    }
}

