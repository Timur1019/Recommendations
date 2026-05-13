package com.kurashnation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kurashnation.dto.response.TrainingRequestRowResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.TrainingRequest;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.TrainingRequestRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.TrainingRequestListService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingRequestListServiceImpl implements TrainingRequestListService {

    private static final int ADMIN_PAGE_SIZE = 200;

    private final UserRepository userRepository;
    private final TrainingRequestRepository trainingRequestRepository;
    private final AthleteProfileProvisioning athleteProfileProvisioning;
    private final CoachProfileProvisioning coachProfileProvisioning;

    public TrainingRequestListServiceImpl(
            UserRepository userRepository,
            TrainingRequestRepository trainingRequestRepository,
            AthleteProfileProvisioning athleteProfileProvisioning,
            CoachProfileProvisioning coachProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.trainingRequestRepository = trainingRequestRepository;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
        this.coachProfileProvisioning = coachProfileProvisioning;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRequestRowResponse> listMine(String email) {
        Athlete athlete = requireAthleteByEmail(email);
        return trainingRequestRepository.findAllByAthlete_IdOrderByCreatedAtDesc(athlete.getId()).stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRequestRowResponse> listForCoach(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.COACH) {
            throw new ValidationException("Access denied");
        }
        Coach coach = coachProfileProvisioning.ensureCoachProfile(user);
        return trainingRequestRepository.findAllByAthlete_Coach_IdOrderByCreatedAtDesc(coach.getId()).stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRequestRowResponse> listForAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Access denied");
        }
        return trainingRequestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, ADMIN_PAGE_SIZE))
                .getContent()
                .stream()
                .map(this::toRow)
                .toList();
    }

    private Athlete requireAthleteByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) {
            throw new ValidationException("Access denied");
        }
        return athleteProfileProvisioning.ensureAthleteProfile(user);
    }

    private TrainingRequestRowResponse toRow(TrainingRequest tr) {
        Athlete a = tr.getAthlete();
        User athleteUser = a.getUser();
        Long coachId = null;
        String coachName = null;
        if (a.getCoach() != null) {
            coachId = a.getCoach().getId();
            User coachUser = a.getCoach().getUser();
            if (coachUser != null) {
                coachName = coachUser.getFullName();
            }
        }
        return new TrainingRequestRowResponse(
                tr.getId(),
                tr.getStatus().name(),
                tr.getCreatedAt(),
                extractNote(tr),
                a.getId(),
                athleteUser != null ? athleteUser.getFullName() : null,
                coachId,
                coachName
        );
    }

    private static String extractNote(TrainingRequest tr) {
        JsonNode n = tr.getProposedData();
        if (n == null || !n.hasNonNull("note")) {
            return null;
        }
        String text = n.get("note").asText();
        return text.isBlank() ? null : text;
    }
}
