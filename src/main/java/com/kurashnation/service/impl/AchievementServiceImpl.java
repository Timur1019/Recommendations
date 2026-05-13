package com.kurashnation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kurashnation.dto.request.AchievementBatchRequest;
import com.kurashnation.dto.request.AchievementItemRequest;
import com.kurashnation.dto.request.AchievementRequest;
import com.kurashnation.dto.request.AchievementWeekRequest;
import com.kurashnation.dto.request.TimeActivityRequest;
import com.kurashnation.dto.request.WeekDayScheduleRequest;
import com.kurashnation.dto.response.AchievementResponse;
import com.kurashnation.dto.response.AchievementWeekResponse;
import com.kurashnation.dto.response.DayScheduleResponse;
import com.kurashnation.dto.response.TimeActivityResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Achievement;
import com.kurashnation.model.entity.AchievementTrainingWeek;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AchievementRepository;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.AchievementMediaService;
import com.kurashnation.service.interfaces.AchievementService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final AchievementRepository achievementRepository;
    private final ObjectMapper objectMapper;
    private final AchievementMediaService achievementMediaService;
    private final AthleteProfileProvisioning athleteProfileProvisioning;

    public AchievementServiceImpl(
            UserRepository userRepository,
            AthleteRepository athleteRepository,
            AchievementRepository achievementRepository,
            ObjectMapper objectMapper,
            AchievementMediaService achievementMediaService,
            AthleteProfileProvisioning athleteProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.athleteRepository = athleteRepository;
        this.achievementRepository = achievementRepository;
        this.objectMapper = objectMapper;
        this.achievementMediaService = achievementMediaService;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
    }

    @Override
    public List<AchievementResponse> myAchievements(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) throw new ValidationException("Access denied");
        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);
        return achievementRepository.findAllByAthleteIdWithWeeksOrderByCompetitionDateDesc(athlete.getId())
                .stream()
                .map(this::toFullResponse)
                .toList();
    }

    @Override
    @Transactional
    public AchievementResponse requestAddAchievement(String email, AchievementRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) throw new ValidationException("Access denied");

        Athlete me = athleteProfileProvisioning.ensureAthleteProfile(user);
        if (!me.getId().equals(request.athleteId())) throw new ValidationException("Access denied");

        Achievement achievement = new Achievement();
        achievement.setAthlete(me);
        achievement.setCompetitionName(request.competitionName());
        achievement.setCompetitionDate(request.competitionDate());
        achievement.setCompetitionLevel(request.competitionLevel());
        achievement.setMedalType(request.medalType());
        achievement.setMedalPhotoUrl(request.medalPhotoUrl());
        achievement.setVerifiedByAdmin(false);

        achievement = achievementRepository.save(achievement);
        if (request.weeks() != null && !request.weeks().isEmpty()) {
            attachWeeks(achievement, request.weeks());
        }
        achievement = achievementRepository.save(achievement);
        LogUtil.info("Achievement requested id=%s athleteId=%s", achievement.getId(), me.getId());
        return toFullResponse(achievement);
    }

    @Override
    @Transactional
    public List<AchievementResponse> requestBatchAchievements(String email, AchievementBatchRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) throw new ValidationException("Access denied");

        Athlete me = athleteProfileProvisioning.ensureAthleteProfile(user);
        if (!me.getId().equals(request.athleteId())) throw new ValidationException("Access denied");

        List<AchievementResponse> out = new ArrayList<>();
        for (AchievementItemRequest item : request.items()) {
            Achievement achievement = new Achievement();
            achievement.setAthlete(me);
            achievement.setCompetitionName(item.competitionName());
            achievement.setCompetitionDate(item.competitionDate());
            achievement.setCompetitionLevel(item.competitionLevel());
            achievement.setMedalType(item.medalType());
            achievement.setMedalPhotoUrl(item.medalPhotoUrl());
            achievement.setVerifiedByAdmin(false);
            achievement = achievementRepository.save(achievement);
            if (item.weeks() != null && !item.weeks().isEmpty()) {
                attachWeeks(achievement, item.weeks());
            }
            achievement = achievementRepository.save(achievement);
            out.add(toFullResponse(achievement));
        }
        LogUtil.info("Achievement batch created count=%s athleteId=%s", out.size(), me.getId());
        return out;
    }

    @Override
    @Transactional
    public AchievementResponse updateAchievement(String email, Long id, AchievementRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.COACH)) throw new ValidationException("Access denied");

        Achievement achievement = achievementRepository.findById(id).orElseThrow(() -> new NotFoundException("Achievement not found"));
        Athlete athlete = athleteRepository.findById(request.athleteId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));

        achievement.setAthlete(athlete);
        achievement.setCompetitionName(request.competitionName());
        achievement.setCompetitionDate(request.competitionDate());
        achievement.setCompetitionLevel(request.competitionLevel());
        achievement.setMedalType(request.medalType());
        achievement.setMedalPhotoUrl(request.medalPhotoUrl());

        achievement.getTrainingWeeks().clear();
        if (request.weeks() != null && !request.weeks().isEmpty()) {
            attachWeeks(achievement, request.weeks());
        }

        achievement = achievementRepository.save(achievement);
        LogUtil.info("Achievement updated id=%s byUserId=%s", id, user.getId());
        return toFullResponse(achievement);
    }

    @Override
    @Transactional
    public void deleteAchievement(String email, Long id) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ADMIN) throw new ValidationException("Access denied");

        if (!achievementRepository.existsById(id)) throw new NotFoundException("Achievement not found");
        achievementMediaService.deleteAllForAchievement(id);
        achievementRepository.deleteById(id);
        LogUtil.warn("Achievement deleted id=%s byUserId=%s", id, user.getId());
    }

    @Override
    @Transactional
    public AchievementResponse verifyByAdmin(String email, Long id) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ADMIN) throw new ValidationException("Access denied");

        Achievement achievement = achievementRepository.findById(id).orElseThrow(() -> new NotFoundException("Achievement not found"));
        achievement.setVerifiedByAdmin(true);
        achievement = achievementRepository.save(achievement);
        LogUtil.warn("Achievement verified id=%s byUserId=%s", id, user.getId());
        return toFullResponse(achievement);
    }

    private void attachWeeks(Achievement achievement, List<AchievementWeekRequest> weeks) {
        for (AchievementWeekRequest w : weeks) {
            AchievementTrainingWeek tw = new AchievementTrainingWeek();
            tw.setAchievement(achievement);
            tw.setWeekStartDate(w.weekStartDate());
            tw.setScheduleJson(buildScheduleJson(w.days()));
            achievement.getTrainingWeeks().add(tw);
        }
    }

    private JsonNode buildScheduleJson(List<WeekDayScheduleRequest> days) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode arr = root.putArray("days");
        for (WeekDayScheduleRequest d : days) {
            DayOfWeek dow;
            try {
                dow = DayOfWeek.valueOf(d.dayOfWeek().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid dayOfWeek: " + d.dayOfWeek());
            }
            ObjectNode dayNode = objectMapper.createObjectNode();
            dayNode.put("dayOfWeek", dow.name());
            ArrayNode entries = dayNode.putArray("entries");
            for (TimeActivityRequest t : d.entries()) {
                ObjectNode e = objectMapper.createObjectNode();
                e.put("time", t.time());
                e.put("activity", t.activity());
                entries.add(e);
            }
            arr.add(dayNode);
        }
        return root;
    }

    private AchievementResponse toFullResponse(Achievement a) {
        List<AchievementWeekResponse> weeks = a.getTrainingWeeks() == null ? List.of() : a.getTrainingWeeks().stream()
                .sorted(Comparator.comparing(AchievementTrainingWeek::getWeekStartDate))
                .map(this::mapWeekEntity)
                .toList();
        return new AchievementResponse(
                a.getId(),
                a.getAthlete().getId(),
                a.getCompetitionName(),
                a.getCompetitionDate(),
                a.getCompetitionLevel().name(),
                a.getMedalType().name(),
                a.getMedalPhotoUrl(),
                a.isVerifiedByAdmin(),
                weeks,
                achievementMediaService.listDtosForAchievement(a.getId())
        );
    }

    private AchievementWeekResponse mapWeekEntity(AchievementTrainingWeek w) {
        return new AchievementWeekResponse(w.getId(), w.getWeekStartDate(), parseSchedule(w.getScheduleJson()));
    }

    private List<DayScheduleResponse> parseSchedule(JsonNode json) {
        if (json == null || !json.has("days") || !json.get("days").isArray()) {
            return List.of();
        }
        List<DayScheduleResponse> out = new ArrayList<>();
        for (JsonNode day : json.get("days")) {
            String dow = day.path("dayOfWeek").asText("");
            List<TimeActivityResponse> entries = new ArrayList<>();
            JsonNode entArr = day.get("entries");
            if (entArr != null && entArr.isArray()) {
                for (JsonNode e : entArr) {
                    entries.add(new TimeActivityResponse(
                            e.path("time").asText(""),
                            e.path("activity").asText("")
                    ));
                }
            }
            out.add(new DayScheduleResponse(dow, entries));
        }
        return out;
    }
}
