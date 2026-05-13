package com.kurashnation.controller;

import com.kurashnation.dto.request.CoachAssignmentRequest;
import com.kurashnation.dto.response.AchievementResponse;
import com.kurashnation.dto.response.AthleteResponse;
import com.kurashnation.dto.response.CoachOptionResponse;
import com.kurashnation.dto.response.UserResponse;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.service.interfaces.AchievementService;
import com.kurashnation.service.interfaces.AthleteService;
import com.kurashnation.service.interfaces.CoachService;
import com.kurashnation.service.interfaces.GoldStandardCalculatorService;
import com.kurashnation.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AchievementService achievementService;
    private final GoldStandardCalculatorService goldStandardCalculatorService;
    private final AthleteService athleteService;
    private final CoachService coachService;

    public AdminController(
            UserService userService,
            AchievementService achievementService,
            GoldStandardCalculatorService goldStandardCalculatorService,
            AthleteService athleteService,
            CoachService coachService
    ) {
        this.userService = userService;
        this.achievementService = achievementService;
        this.goldStandardCalculatorService = goldStandardCalculatorService;
        this.athleteService = athleteService;
        this.coachService = coachService;
    }

    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "Users list")
    @GetMapping("/users")
    public List<UserResponse> users() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Change user role")
    @ApiResponse(responseCode = "200", description = "Updated user")
    @PutMapping("/users/{id}/role")
    public UserResponse updateRole(@PathVariable("id") Long id, @RequestBody @NotNull RoleRequest request) {
        return userService.updateRole(id, request.role());
    }

    @Operation(summary = "Activate/deactivate user")
    @ApiResponse(responseCode = "200", description = "Updated user")
    @PutMapping("/users/{id}/activate")
    public UserResponse setActive(@PathVariable("id") Long id, @RequestBody @NotNull ActivateRequest request) {
        return userService.setActive(id, request.active());
    }

    @Operation(summary = "Verify achievement by id")
    @ApiResponse(responseCode = "200", description = "Achievement verified")
    @PostMapping("/achievements/verify/{id}")
    public AchievementResponse verifyAchievement(Authentication authentication, @PathVariable("id") Long id) {
        return achievementService.verifyByAdmin(authentication.getName(), id);
    }

    @Operation(summary = "Recalculate gold standards")
    @ApiResponse(responseCode = "200", description = "Recalculation started")
    @PostMapping("/gold-standard/recalculate")
    public void recalculate() {
        goldStandardCalculatorService.recalculateAll();
    }

    @Operation(summary = "List coaches (for assigning athletes)")
    @ApiResponse(responseCode = "200", description = "Coach options")
    @GetMapping("/coaches")
    public List<CoachOptionResponse> listCoaches() {
        return coachService.listAllForAdmin();
    }

    @Operation(summary = "Assign or clear coach for athlete")
    @ApiResponse(responseCode = "200", description = "Updated athlete")
    @PatchMapping("/athletes/{athleteId}/coach")
    public AthleteResponse assignAthleteCoach(
            @PathVariable("athleteId") Long athleteId,
            @RequestBody CoachAssignmentRequest request
    ) {
        return athleteService.adminAssignCoach(athleteId, request.coachId());
    }

    public record RoleRequest(@NotNull UserRole role) {
    }

    public record ActivateRequest(@NotNull Boolean active) {
    }
}

