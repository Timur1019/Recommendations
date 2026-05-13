package com.kurashnation.service.impl;

import com.kurashnation.dto.request.AthleteNameRequest;
import com.kurashnation.dto.request.UpsertAthleteRequest;
import com.kurashnation.dto.response.AthleteResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.mapper.AthleteMapper;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.AthleteService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AthleteServiceImpl implements AthleteService {

    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final CoachRepository coachRepository;
    private final AthleteMapper athleteMapper;
    private final AthleteProfileProvisioning athleteProfileProvisioning;
    private final CoachProfileProvisioning coachProfileProvisioning;

    public AthleteServiceImpl(
            UserRepository userRepository,
            AthleteRepository athleteRepository,
            CoachRepository coachRepository,
            AthleteMapper athleteMapper,
            AthleteProfileProvisioning athleteProfileProvisioning,
            CoachProfileProvisioning coachProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.athleteRepository = athleteRepository;
        this.coachRepository = coachRepository;
        this.athleteMapper = athleteMapper;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
        this.coachProfileProvisioning = coachProfileProvisioning;
    }

    @Override
    @Transactional(readOnly = true)
    public AthleteResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) {
            throw new ValidationException("Профиль спортсмена доступен только для роли ATHLETE");
        }
        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);
        athlete = athleteRepository.findByIdFetchUserAndCoach(athlete.getId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));
        return athleteMapper.toDto(athlete);
    }

    @Override
    @Transactional
    public AthleteResponse updateMyProfile(String email, UpsertAthleteRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);
        athlete = athleteRepository.findByIdFetchUserAndCoach(athlete.getId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));

        user.setFullName(request.fullName());
        user.setPhone(request.phone());

        athlete.setRegion(request.region());
        athlete.setWeightCategory(request.weightCategory());
        athlete.setDateOfBirth(request.dateOfBirth());
        athlete.setRank(request.rank());

        if (request.currentMedalCountGold() != null) athlete.setCurrentMedalCountGold(request.currentMedalCountGold());
        if (request.currentMedalCountSilver() != null)
            athlete.setCurrentMedalCountSilver(request.currentMedalCountSilver());
        if (request.currentMedalCountBronze() != null)
            athlete.setCurrentMedalCountBronze(request.currentMedalCountBronze());

        if (request.sportType() != null && !request.sportType().isBlank()) {
            athlete.setSportType(request.sportType().trim());
        }
        athlete.setHeightCm(request.heightCm());
        athlete.setBodyWeightKg(request.bodyWeightKg());
        if (request.goalText() != null) {
            athlete.setGoalText(request.goalText().isBlank() ? null : request.goalText().trim());
        }

        LogUtil.info("Athlete updated athleteId=%s userId=%s", athlete.getId(), user.getId());
        return athleteMapper.toDto(athlete);
    }

    @Override
    @Transactional
    public AthleteResponse patchMyDisplayName(String email, AthleteNameRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);
        athlete = athleteRepository.findByIdFetchUserAndCoach(athlete.getId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));
        String first = request.firstName().trim();
        String last = request.lastName().trim();
        user.setFullName(first + " " + last);
        LogUtil.info("Athlete display name updated athleteId=%s", athlete.getId());
        return athleteMapper.toDto(athlete);
    }

    @Override
    @Transactional(readOnly = true)
    public AthleteResponse getAthleteById(String email, Long athleteId) {
        User requester = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        Athlete athlete = athleteRepository.findById(athleteId).orElseThrow(() -> new NotFoundException("Athlete not found"));

        if (requester.getRole() == UserRole.ADMIN) {
            return athleteMapper.toDto(athlete);
        }

        if (requester.getRole() == UserRole.ATHLETE) {
            Athlete me = athleteProfileProvisioning.ensureAthleteProfile(requester);
            if (!me.getId().equals(athleteId)) throw new ValidationException("Access denied");
            return athleteMapper.toDto(athlete);
        }

        if (requester.getRole() == UserRole.COACH) {
            Coach coach = coachProfileProvisioning.ensureCoachProfile(requester);
            if (athlete.getCoach() == null || !coach.getId().equals(athlete.getCoach().getId())) {
                throw new ValidationException("Access denied");
            }
            return athleteMapper.toDto(athlete);
        }

        throw new ValidationException("Access denied");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AthleteResponse> getAthletesByCoach(String email, Long coachId) {
        User requester = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (requester.getRole() == UserRole.COACH) {
            Coach coach = coachProfileProvisioning.ensureCoachProfile(requester);
            if (!coach.getId().equals(coachId)) throw new ValidationException("Access denied");
        } else if (requester.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Access denied");
        }

        return athleteRepository.findAll().stream()
                .filter(a -> a.getCoach() != null && coachId.equals(a.getCoach().getId()))
                .map(athleteMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AthleteResponse adminAssignCoach(Long athleteId, Long coachId) {
        Athlete athlete = athleteRepository.findById(athleteId).orElseThrow(() -> new NotFoundException("Athlete not found"));
        if (coachId == null) {
            athlete.setCoach(null);
        } else {
            Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new ValidationException("Coach not found"));
            athlete.setCoach(coach);
        }
        LogUtil.info("Admin assigned coach athleteId=%s coachId=%s", athleteId, coachId);
        return athleteMapper.toDto(athlete);
    }
}

