package com.kurashnation.service.impl;

import com.kurashnation.dto.response.UserResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.mapper.UserMappingHelper;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.UserService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final UserMappingHelper userMappingHelper;
    private final CoachProfileProvisioning coachProfileProvisioning;

    public UserServiceImpl(
            UserRepository userRepository,
            AthleteRepository athleteRepository,
            UserMappingHelper userMappingHelper,
            CoachProfileProvisioning coachProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.athleteRepository = athleteRepository;
        this.userMappingHelper = userMappingHelper;
        this.coachProfileProvisioning = coachProfileProvisioning;
    }

    @Override
    public User requireActiveUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.isActive()) throw new ValidationException("User is inactive");
        return user;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(userMappingHelper::userToDto).toList();
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setRole(role);
        if (role == UserRole.ATHLETE && athleteRepository.findByUserId(user.getId()).isEmpty()) {
            Athlete athlete = new Athlete();
            athlete.setUser(user);
            athlete.setRegion("TASHKENT");
            athlete.setWeightCategory("-81");
            athlete.setDateOfBirth(LocalDate.of(2000, 1, 1));
            athlete.setSportType("KURASH");
            athleteRepository.save(athlete);
            LogUtil.warn("Created athlete profile stub for userId=%s (role set to ATHLETE)", userId);
        }
        if (role == UserRole.COACH) {
            coachProfileProvisioning.ensureCoachProfile(user);
        }
        LogUtil.warn("User role updated userId=%s role=%s", userId, role);
        return userMappingHelper.userToDto(user);
    }

    @Override
    @Transactional
    public UserResponse setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        user.setActive(active);
        LogUtil.warn("User active updated userId=%s active=%s", userId, active);
        return userMappingHelper.userToDto(user);
    }
}

