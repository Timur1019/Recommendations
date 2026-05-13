package com.kurashnation.service.impl;

import com.kurashnation.dto.request.LoginRequest;
import com.kurashnation.dto.request.RegisterAthleteRequest;
import com.kurashnation.dto.request.RegisterCoachRequest;
import com.kurashnation.dto.request.UserProfileUpdateRequest;
import com.kurashnation.dto.response.LoginResponse;
import com.kurashnation.dto.response.UserResponse;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.mapper.UserMappingHelper;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.security.JwtTokenService;
import com.kurashnation.service.interfaces.AuthService;
import com.kurashnation.util.LogUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final CoachRepository coachRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final UserMappingHelper userMappingHelper;
    private final Validator validator;

    public AuthServiceImpl(
            UserRepository userRepository,
            AthleteRepository athleteRepository,
            CoachRepository coachRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            UserMappingHelper userMappingHelper,
            Validator validator
    ) {
        this.userRepository = userRepository;
        this.athleteRepository = athleteRepository;
        this.coachRepository = coachRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.userMappingHelper = userMappingHelper;
        this.validator = validator;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!user.isActive()) {
            throw new ValidationException("User is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new LoginResponse(token, userMappingHelper.userToDto(user));
    }

    @Override
    @Transactional
    public UserResponse registerAthlete(RegisterAthleteRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(UserRole.ATHLETE);
        user.setActive(true);
        user = userRepository.save(user);

        Coach coach = null;
        if (request.coachId() != null) {
            coach = coachRepository.findById(request.coachId())
                    .orElseThrow(() -> new ValidationException("Coach not found"));
        }

        Athlete athlete = new Athlete();
        athlete.setUser(user);
        athlete.setCoach(coach);
        athlete.setRegion(request.region());
        athlete.setWeightCategory(request.weightCategory());
        athlete.setDateOfBirth(request.dateOfBirth());
        athlete.setRank(request.rank());
        athleteRepository.save(athlete);

        LogUtil.info("Registered athlete userId=%s email=%s", user.getId(), user.getEmail());
        return userMappingHelper.userToDto(user);
    }

    @Override
    @Transactional
    public UserResponse registerCoach(RegisterCoachRequest raw) {
        RegisterCoachRequest request = normalizeRegisterCoach(raw);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            throw new ValidationException(msg.isBlank() ? "Validation failed" : msg);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(UserRole.COACH);
        user.setActive(true);
        user = userRepository.save(user);

        Coach coach = new Coach();
        coach.setUser(user);
        coach.setRegion(request.region());
        coach.setFederationMember(Boolean.TRUE.equals(request.federationMember()));
        coach.setExperienceYears(request.experienceYears());
        coachRepository.save(coach);

        LogUtil.info("Registered coach userId=%s email=%s", user.getId(), user.getEmail());
        return userMappingHelper.userToDto(user);
    }

    private static RegisterCoachRequest normalizeRegisterCoach(RegisterCoachRequest r) {
        if (r == null) {
            return new RegisterCoachRequest(null, null, null, null, null, null, null);
        }
        String email = r.email() == null ? null : r.email().trim();
        String fullName = r.fullName() == null ? null : r.fullName().trim();
        String phone = r.phone() == null || r.phone().isBlank() ? null : r.phone().trim();
        String region = r.region() == null || r.region().isBlank() ? null : r.region().trim();
        return new RegisterCoachRequest(
                email,
                r.password(),
                fullName,
                phone,
                region,
                Boolean.TRUE.equals(r.federationMember()),
                r.experienceYears()
        );
    }

    @Override
    @Transactional
    public UserResponse updateMyUserProfile(String email, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ValidationException("User not found"));
        if (request.avatarUrl() != null) {
            String v = request.avatarUrl().trim();
            user.setAvatarUrl(v.isEmpty() ? null : v);
        }
        if (request.phone() != null) {
            String v = request.phone().trim();
            user.setPhone(v.isEmpty() ? null : v);
        }
        LogUtil.info("User profile updated userId=%s", user.getId());
        return userMappingHelper.userToDto(user);
    }
}

