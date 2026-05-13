package com.kurashnation.service.impl;

import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.FitnessTest;
import com.kurashnation.model.entity.GoldStandard;
import com.kurashnation.model.entity.Training;
import com.kurashnation.model.enums.MedalType;
import com.kurashnation.repository.AchievementRepository;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.FitnessTestRepository;
import com.kurashnation.repository.GoldStandardRepository;
import com.kurashnation.repository.TrainingRepository;
import com.kurashnation.service.interfaces.GoldStandardCalculatorService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoldStandardCalculatorServiceImpl implements GoldStandardCalculatorService {

    private final AchievementRepository achievementRepository;
    private final AthleteRepository athleteRepository;
    private final TrainingRepository trainingRepository;
    private final FitnessTestRepository fitnessTestRepository;
    private final GoldStandardRepository goldStandardRepository;

    public GoldStandardCalculatorServiceImpl(
            AchievementRepository achievementRepository,
            AthleteRepository athleteRepository,
            TrainingRepository trainingRepository,
            FitnessTestRepository fitnessTestRepository,
            GoldStandardRepository goldStandardRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.athleteRepository = athleteRepository;
        this.trainingRepository = trainingRepository;
        this.fitnessTestRepository = fitnessTestRepository;
        this.goldStandardRepository = goldStandardRepository;
    }

    @Override
    @Transactional
    public void recalculateAll() {
        LocalDate from = LocalDate.now().minusYears(10); // pragmatic
        List<Long> goldAthleteIds = achievementRepository.findDistinctAthleteIdsWithMedalSince(MedalType.GOLD, from);
        if (goldAthleteIds.isEmpty()) {
            LogUtil.warn("Gold standard recalculation skipped: no verified gold achievements");
            return;
        }

        List<Athlete> goldAthletes = athleteRepository.findAllByIdIn(goldAthleteIds);
        Map<String, List<Athlete>> byCategory = goldAthletes.stream()
                .collect(Collectors.groupingBy(Athlete::getWeightCategory));

        for (Map.Entry<String, List<Athlete>> e : byCategory.entrySet()) {
            calculateAndSaveForCategory(e.getKey(), e.getValue());
        }

        LogUtil.warn("Gold standards recalculated categories=%s", byCategory.keySet().size());
    }

    private void calculateAndSaveForCategory(String weightCategory, List<Athlete> athletes) {
        LocalDate trainingsFrom = LocalDate.now().minusMonths(3);
        LocalDate testsFrom = LocalDate.now().minusMonths(6);

        List<Long> ids = athletes.stream().map(Athlete::getId).toList();

        List<Training> trainings = trainingRepository.findAll().stream()
                .filter(t -> ids.contains(t.getAthlete().getId()) && !t.getTrainingDate().isBefore(trainingsFrom))
                .toList();

        List<FitnessTest> tests = fitnessTestRepository.findAll().stream()
                .filter(ft -> ids.contains(ft.getAthlete().getId()) && !ft.getTestDate().isBefore(testsFrom))
                .toList();

        BigDecimal avgTrainingsPerWeek = trainings.isEmpty() ? null :
                bd(((double) trainings.size()) / 12.0, 2);
        BigDecimal avgIntensity = trainings.stream().filter(t -> t.getIntensity() != null).mapToInt(Training::getIntensity).average()
                .stream().boxed().findFirst().map(v -> bd(v, 2)).orElse(null);

        Integer avgPullups = tests.stream().filter(t -> t.getPullupsCount() != null).mapToInt(FitnessTest::getPullupsCount).boxed()
                .sorted(Comparator.reverseOrder()).findFirst().orElse(null);

        GoldStandard standard = goldStandardRepository.findByWeightCategory(weightCategory).orElseGet(GoldStandard::new);
        standard.setWeightCategory(weightCategory);
        standard.setAvgTrainingsPerWeek(avgTrainingsPerWeek);
        standard.setAvgIntensity(avgIntensity);
        standard.setAvgPullups(avgPullups);
        goldStandardRepository.save(standard);
    }

    private static BigDecimal bd(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP);
    }
}

