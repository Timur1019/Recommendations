package com.kurashnation.service.impl;

import com.kurashnation.dto.response.CoachOptionResponse;
import com.kurashnation.dto.response.CoachResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.mapper.CoachMapper;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.CoachService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CoachServiceImpl implements CoachService {

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;
    private final CoachProfileProvisioning coachProfileProvisioning;

    public CoachServiceImpl(
            UserRepository userRepository,
            CoachRepository coachRepository,
            CoachMapper coachMapper,
            CoachProfileProvisioning coachProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.coachRepository = coachRepository;
        this.coachMapper = coachMapper;
        this.coachProfileProvisioning = coachProfileProvisioning;
    }

    @Override
    @Transactional(readOnly = true)
    public CoachResponse myProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.COACH) throw new ValidationException("Access denied");
        Coach coach = coachProfileProvisioning.ensureCoachProfile(user);
        coach = coachRepository.findByIdFetchUser(coach.getId())
                .orElseThrow(() -> new NotFoundException("Coach not found"));
        return coachMapper.toDto(coach);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachOptionResponse> listAllForAdmin() {
        return coachRepository.findAll().stream()
                .map(c -> new CoachOptionResponse(
                        c.getId(),
                        c.getUser().getFullName(),
                        c.getUser().getEmail()))
                .sorted(Comparator.comparing(CoachOptionResponse::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}

