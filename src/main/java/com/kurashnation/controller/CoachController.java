package com.kurashnation.controller;

import com.kurashnation.dto.response.CoachResponse;
import com.kurashnation.service.interfaces.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coaches")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @Operation(summary = "My coach profile")
    @ApiResponse(responseCode = "200", description = "Coach profile")
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/me")
    public CoachResponse me(Authentication authentication) {
        return coachService.myProfile(authentication.getName());
    }
}

