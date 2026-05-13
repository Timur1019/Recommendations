package com.kurashnation.controller;

import com.kurashnation.dto.request.LoginRequest;
import com.kurashnation.dto.request.RegisterAthleteRequest;
import com.kurashnation.dto.request.RegisterCoachRequest;
import com.kurashnation.dto.request.UserProfileUpdateRequest;
import com.kurashnation.dto.response.LoginResponse;
import com.kurashnation.dto.response.UserResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.mapper.UserMappingHelper;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * {@code @CrossOrigin} registers CORS preflight (OPTIONS) for {@code /auth/**} in Spring MVC so a missing
 * servlet CORS match (OPTIONS) does not fall through as 405.
 */
@CrossOrigin(
        originPatterns = {
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://[::1]:*",
                "http://[0:0:0:0:0:0:0:1]:*",
                "https://localhost:*",
                "https://127.0.0.1:*"
        },
        allowCredentials = "true",
        maxAge = 3600
)
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserMappingHelper userMappingHelper;

    public AuthController(AuthService authService, UserRepository userRepository, UserMappingHelper userMappingHelper) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.userMappingHelper = userMappingHelper;
    }

    @Operation(summary = "Login (email/password)")
    @ApiResponse(responseCode = "200", description = "JWT token issued")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Register athlete")
    @ApiResponse(responseCode = "200", description = "Athlete registered")
    @PostMapping("/register/athlete")
    public UserResponse registerAthlete(@Valid @RequestBody RegisterAthleteRequest request) {
        return authService.registerAthlete(request);
    }

    @Operation(summary = "Register coach")
    @ApiResponse(responseCode = "200", description = "Coach registered")
    @PostMapping("/register/coach")
    public UserResponse registerCoach(@RequestBody RegisterCoachRequest request) {
        return authService.registerCoach(request);
    }

    @Operation(summary = "Current user")
    @ApiResponse(responseCode = "200", description = "User info")
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(userMappingHelper::userToDto)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(summary = "Update avatar URL and phone")
    @ApiResponse(responseCode = "200", description = "Updated user")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/profile")
    public UserResponse patchMyProfile(Authentication authentication, @Valid @RequestBody UserProfileUpdateRequest request) {
        return authService.updateMyUserProfile(authentication.getName(), request);
    }
}

