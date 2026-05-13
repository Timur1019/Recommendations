package com.kurashnation.mapper;

import com.kurashnation.dto.response.UserResponse;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class UserMappingHelper {

    private final AthleteRepository athleteRepository;

    public UserMappingHelper(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
    }

    @Named("userToDto")
    public UserResponse userToDto(User user) {
        if (user == null) {
            return null;
        }
        Long athleteId = null;
        Long coachId = null;
        if (user.getRole() == UserRole.ATHLETE) {
            Athlete athlete = athleteRepository.findByUserId(user.getId()).orElse(null);
            if (athlete != null) {
                athleteId = athlete.getId();
                coachId = athlete.getCoach() == null ? null : athlete.getCoach().getId();
            }
        }
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getRole().name(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.isActive(),
                athleteId,
                coachId
        );
    }
}
