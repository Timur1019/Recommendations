package com.kurashnation.service.impl;

import com.kurashnation.dto.request.TrainingRequest;
import com.kurashnation.dto.response.TrainingResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.mapper.TrainingMapper;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.Training;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.TrainingRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.TrainingService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainingServiceImpl implements TrainingService {

    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final TrainingRepository trainingRepository;
    private final TrainingMapper trainingMapper;
    private final AthleteProfileProvisioning athleteProfileProvisioning;
    private final CoachProfileProvisioning coachProfileProvisioning;

    public TrainingServiceImpl(
            UserRepository userRepository,
            AthleteRepository athleteRepository,
            TrainingRepository trainingRepository,
            TrainingMapper trainingMapper,
            AthleteProfileProvisioning athleteProfileProvisioning,
            CoachProfileProvisioning coachProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.athleteRepository = athleteRepository;
        this.trainingRepository = trainingRepository;
        this.trainingMapper = trainingMapper;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
        this.coachProfileProvisioning = coachProfileProvisioning;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingResponse> getMyTrainings(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getRole() == UserRole.ATHLETE) {
            Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);
            return trainingRepository.findAllByAthleteIdOrderByTrainingDateDesc(athlete.getId())
                    .stream().map(trainingMapper::toDto).toList();
        }

        if (user.getRole() == UserRole.COACH) {
            Coach coach = coachProfileProvisioning.ensureCoachProfile(user);
            return trainingRepository.findAllByAthlete_Coach_IdOrderByTrainingDateDesc(coach.getId()).stream()
                    .map(trainingMapper::toDto)
                    .toList();
        }

        throw new ValidationException("Access denied");
    }

    @Override
    @Transactional
    public TrainingResponse createTraining(String email, TrainingRequest request) {
        User creator = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(creator.getRole() == UserRole.COACH || creator.getRole() == UserRole.ADMIN)) {
            throw new ValidationException("Access denied");
        }

        Athlete athlete = athleteRepository.findById(request.athleteId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));

        if (creator.getRole() == UserRole.COACH) {
            Coach coach = coachProfileProvisioning.ensureCoachProfile(creator);
            assertCoachManagesAthlete(coach, athlete);
        }

        Training training = new Training();
        training.setAthlete(athlete);
        training.setTrainingDate(request.trainingDate());
        training.setWorkoutType(request.workoutType());
        training.setDurationMinutes(request.durationMinutes());
        training.setIntensity(request.intensity());
        training.setTechnicalActions(request.technicalActions());
        training.setNotes(request.notes());
        training.setCreatedBy(creator);

        training = trainingRepository.save(training);
        LogUtil.info("Training created id=%s athleteId=%s byUserId=%s", training.getId(), athlete.getId(), creator.getId());
        return trainingMapper.toDto(training);
    }

    @Override
    @Transactional
    public TrainingResponse updateTraining(String email, Long trainingId, TrainingRequest request) {
        User editor = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(editor.getRole() == UserRole.COACH || editor.getRole() == UserRole.ADMIN)) {
            throw new ValidationException("Access denied");
        }

        Training training = trainingRepository.findById(trainingId).orElseThrow(() -> new NotFoundException("Training not found"));

        Athlete athlete = athleteRepository.findById(request.athleteId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));

        if (editor.getRole() == UserRole.COACH) {
            Coach coach = coachProfileProvisioning.ensureCoachProfile(editor);
            assertCoachManagesAthlete(coach, training.getAthlete());
            assertCoachManagesAthlete(coach, athlete);
        }

        training.setAthlete(athlete);
        training.setTrainingDate(request.trainingDate());
        training.setWorkoutType(request.workoutType());
        training.setDurationMinutes(request.durationMinutes());
        training.setIntensity(request.intensity());
        training.setTechnicalActions(request.technicalActions());
        training.setNotes(request.notes());

        LogUtil.info("Training updated id=%s byUserId=%s", trainingId, editor.getId());
        return trainingMapper.toDto(training);
    }

    @Override
    @Transactional
    public void deleteTraining(String email, Long trainingId) {
        User deleter = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (deleter.getRole() != UserRole.ADMIN) throw new ValidationException("Access denied");

        if (!trainingRepository.existsById(trainingId)) throw new NotFoundException("Training not found");
        trainingRepository.deleteById(trainingId);
        LogUtil.warn("Training deleted id=%s byUserId=%s", trainingId, deleter.getId());
    }

    private static void assertCoachManagesAthlete(Coach coach, Athlete athlete) {
        if (athlete.getCoach() == null || !athlete.getCoach().getId().equals(coach.getId())) {
            throw new ValidationException("Coach can only manage trainings for athletes assigned to them");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> weeklyStatistics(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) throw new ValidationException("Access denied");

        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);

        LocalDate from = LocalDate.now().minusDays(6);
        List<Training> trainings = trainingRepository.findAllByAthleteIdSince(athlete.getId(), from);

        int total = trainings.size();
        double avgIntensity = trainings.stream()
                .filter(t -> t.getIntensity() != null)
                .mapToInt(Training::getIntensity)
                .average()
                .orElse(0.0);

        Map<String, Object> resp = new HashMap<>();
        resp.put("from", from.toString());
        resp.put("to", LocalDate.now().toString());
        resp.put("count", total);
        resp.put("avgIntensity", avgIntensity);
        return resp;
    }
}

