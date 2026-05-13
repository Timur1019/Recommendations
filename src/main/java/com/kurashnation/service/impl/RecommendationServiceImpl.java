package com.kurashnation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurashnation.dto.response.DeficitItemResponse;
import com.kurashnation.dto.response.RecommendationResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.FitnessTest;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.GoldStandard;
import com.kurashnation.model.entity.Recommendation;
import com.kurashnation.model.entity.Training;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.repository.FitnessTestRepository;
import com.kurashnation.repository.GoldStandardRepository;
import com.kurashnation.repository.RecommendationRepository;
import com.kurashnation.repository.TrainingRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.RecommendationService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final AthleteRepository athleteRepository;
    private final TrainingRepository trainingRepository;
    private final FitnessTestRepository fitnessTestRepository;
    private final GoldStandardRepository goldStandardRepository;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;
    private final PdfGeneratorService pdfGeneratorService;
    private final AthleteProfileProvisioning athleteProfileProvisioning;

    public RecommendationServiceImpl(
            UserRepository userRepository,
            CoachRepository coachRepository,
            AthleteRepository athleteRepository,
            TrainingRepository trainingRepository,
            FitnessTestRepository fitnessTestRepository,
            GoldStandardRepository goldStandardRepository,
            RecommendationRepository recommendationRepository,
            ObjectMapper objectMapper,
            PdfGeneratorService pdfGeneratorService,
            AthleteProfileProvisioning athleteProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.coachRepository = coachRepository;
        this.athleteRepository = athleteRepository;
        this.trainingRepository = trainingRepository;
        this.fitnessTestRepository = fitnessTestRepository;
        this.goldStandardRepository = goldStandardRepository;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
        this.pdfGeneratorService = pdfGeneratorService;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
    }

    @Override
    public Optional<RecommendationResponse> latestForMe(String email) {
        Athlete athlete = requireAthleteByEmail(email);
        return recommendationRepository.findFirstByAthleteIdOrderByGeneratedDateDesc(athlete.getId())
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public RecommendationResponse generateForMe(String email) {
        Athlete athlete = requireAthleteByEmail(email);
        RecommendationResponse fresh = buildFreshRecommendation(athlete);
        RecommendationResponse saved = persistRecommendation(athlete, fresh);
        LogUtil.info("Recommendation self-generated athleteId=%s", athlete.getId());
        return saved;
    }

    @Override
    @Transactional
    public RecommendationResponse generateForAthlete(String email, Long athleteId) {
        User requester = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.COACH)) {
            throw new ValidationException("Access denied");
        }

        Athlete athlete = athleteRepository.findById(athleteId).orElseThrow(() -> new NotFoundException("Athlete not found"));
        RecommendationResponse fresh = buildFreshRecommendation(athlete);
        RecommendationResponse saved = persistRecommendation(athlete, fresh);

        LogUtil.info("Recommendation generated athleteId=%s byUserId=%s", athleteId, requester.getId());
        return saved;
    }

    @Override
    public Map<String, Object> compareWithGoldStandard(String email) {
        Athlete athlete = requireAthleteByEmail(email);
        Optional<GoldStandard> stdOpt = goldStandardRepository.findByWeightCategory(athlete.getWeightCategory());
        if (stdOpt.isEmpty()) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("weightCategory", athlete.getWeightCategory());
            resp.put("deficits", List.<DeficitItemResponse>of());
            resp.put("progressPercent", 0);
            resp.put("tipOfTheDay",
                    "Для вашей весовой категории в базе нет эталона (gold_standards). "
                            + "Попросите администратора добавить строку для «" + athlete.getWeightCategory()
                            + "» или укажите в профиле категорию, для которой эталон уже есть (например −81).");
            return resp;
        }
        GoldStandard standard = stdOpt.get();
        Metrics metrics = collectMetrics(athlete);
        List<DeficitItemResponse> deficits = computeDeficits(metrics, standard);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("weightCategory", athlete.getWeightCategory());
        resp.put("deficits", deficits);
        resp.put("progressPercent", computeProgress(metrics, standard));
        resp.put("tipOfTheDay", generateTip(deficits));
        return resp;
    }

    @Override
    public Map<String, String> weekPlanForMe(String email) {
        Athlete athlete = requireAthleteByEmail(email);
        return latestForMe(email)
                .map(RecommendationResponse::weekPlan)
                .orElseGet(() -> buildFreshRecommendation(athlete).weekPlan());
    }

    @Override
    public byte[] exportPdfForMe(String email) {
        Athlete athlete = requireAthleteByEmail(email);
        RecommendationResponse data = latestForMe(email).orElseGet(() -> buildFreshRecommendation(athlete));
        return pdfGeneratorService.generateRecommendationPdf(data);
    }

    @Override
    @Transactional
    public RecommendationResponse saveHandbookPlanForAthlete(
            String coachEmail,
            Long athleteId,
            Map<String, String> weekPlan,
            String customTip
    ) {
        User requester = userRepository.findByEmail(coachEmail).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.COACH)) {
            throw new ValidationException("Access denied");
        }
        Athlete athlete = athleteRepository.findByIdFetchUserAndCoach(athleteId)
                .orElseThrow(() -> new NotFoundException("Athlete not found"));
        if (requester.getRole() == UserRole.COACH) {
            Coach coach = coachRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new NotFoundException("Coach profile not found"));
            if (athlete.getCoach() == null || !athlete.getCoach().getId().equals(coach.getId())) {
                throw new ValidationException("Coach can only assign plans to their athletes");
            }
        }
        Map<String, String> normalized = normalizeWeekPlan(weekPlan);
        Recommendation rec = new Recommendation();
        rec.setAthlete(athlete);
        rec.setGeneratedDate(LocalDate.now());
        rec.setProgressPercent(50);
        rec.setWeekPlan(objectMapper.valueToTree(normalized));
        rec.setDeficits(objectMapper.createArrayNode());
        rec.setCustomTip(customTip == null ? null : customTip.trim());
        rec = recommendationRepository.save(rec);
        LogUtil.info("Handbook plan saved athleteId=%s by=%s", athleteId, coachEmail);
        return toResponse(rec);
    }

    private RecommendationResponse buildFreshRecommendation(Athlete athlete) {
        GoldStandard standard = goldStandardRepository.findByWeightCategory(athlete.getWeightCategory())
                .orElseGet(() -> syntheticGoldStandard(athlete.getWeightCategory()));
        Metrics metrics = collectMetrics(athlete);
        List<DeficitItemResponse> deficits = computeDeficits(metrics, standard);
        int progress = computeProgress(metrics, standard);
        Map<String, String> plan = generateWeekPlan(deficits);
        String tip = generateTip(deficits);
        return new RecommendationResponse(LocalDate.now(), progress, deficits, plan, tip);
    }

    private RecommendationResponse persistRecommendation(Athlete athlete, RecommendationResponse fresh) {
        Recommendation rec = new Recommendation();
        rec.setAthlete(athlete);
        rec.setGeneratedDate(fresh.generatedDate());
        rec.setProgressPercent(fresh.progressPercent());
        rec.setWeekPlan(objectMapper.valueToTree(fresh.weekPlan()));
        rec.setDeficits(objectMapper.valueToTree(fresh.deficits()));
        rec = recommendationRepository.save(rec);
        return toResponse(rec);
    }

    /**
     * Если для веса нет строки в БД — используем разумные значения по умолчанию, чтобы «Сгенерировать» и недельный план работали.
     */
    private GoldStandard syntheticGoldStandard(String weightCategory) {
        LogUtil.warn("Gold standard not in DB for category=%s — using synthetic defaults", weightCategory);
        GoldStandard g = new GoldStandard();
        g.setWeightCategory(weightCategory);
        g.setAvgTrainingsPerWeek(new BigDecimal("4.0"));
        g.setAvgIntensity(new BigDecimal("7.0"));
        g.setAvgPullups(15);
        g.setAvgRun30m(new BigDecimal("4.50"));
        g.setAvgRun2000m(new BigDecimal("420.00"));
        g.setAvgBurpees(20);
        g.setAvgCompetitionsPerYear(new BigDecimal("6.0"));
        return g;
    }

    private Athlete requireAthleteByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) throw new ValidationException("Access denied");
        return athleteProfileProvisioning.ensureAthleteProfile(user);
    }

    private Metrics collectMetrics(Athlete athlete) {
        LocalDate from = LocalDate.now().minusMonths(3);
        List<Training> trainings = trainingRepository.findAllByAthleteIdSince(athlete.getId(), from);

        double trainingsPerWeek = trainings.size() / 12.0;
        double avgIntensity = trainings.stream().filter(t -> t.getIntensity() != null).mapToInt(Training::getIntensity).average().orElse(0.0);

        Optional<FitnessTest> latestTest = fitnessTestRepository.findAllByAthleteIdOrderByTestDateDesc(athlete.getId())
                .stream().findFirst();
        int pullups = latestTest.map(t -> t.getPullupsCount() == null ? 0 : t.getPullupsCount()).orElse(0);

        return new Metrics(trainingsPerWeek, avgIntensity, pullups);
    }

    private List<DeficitItemResponse> computeDeficits(Metrics m, GoldStandard s) {
        List<DeficitItemResponse> deficits = new ArrayList<>();

        double targetTrainings = s.getAvgTrainingsPerWeek() == null ? 0.0 : s.getAvgTrainingsPerWeek().doubleValue();
        double targetIntensity = s.getAvgIntensity() == null ? 0.0 : s.getAvgIntensity().doubleValue();
        double targetPullups = s.getAvgPullups() == null ? 0.0 : s.getAvgPullups();

        if (targetPullups > 0) deficits.add(diff("pullups", m.pullups, targetPullups));
        if (targetIntensity > 0) deficits.add(diff("intensity", m.avgIntensity, targetIntensity));
        if (targetTrainings > 0) deficits.add(diff("trainingsPerWeek", m.trainingsPerWeek, targetTrainings));

        return deficits;
    }

    private int computeProgress(Metrics m, GoldStandard s) {
        double targetTrainings = s.getAvgTrainingsPerWeek() == null ? 0.0 : s.getAvgTrainingsPerWeek().doubleValue();
        double targetIntensity = s.getAvgIntensity() == null ? 0.0 : s.getAvgIntensity().doubleValue();
        double targetPullups = s.getAvgPullups() == null ? 0.0 : s.getAvgPullups();

        double score = 0;
        int parts = 0;

        if (targetPullups > 0) {
            score += Math.min(1.0, m.pullups / targetPullups);
            parts++;
        }
        if (targetIntensity > 0) {
            score += Math.min(1.0, m.avgIntensity / targetIntensity);
            parts++;
        }
        if (targetTrainings > 0) {
            score += Math.min(1.0, m.trainingsPerWeek / targetTrainings);
            parts++;
        }

        if (parts == 0) return 0;
        return (int) Math.round((score / parts) * 100.0);
    }

    private Map<String, String> generateWeekPlan(List<DeficitItemResponse> deficits) {
        Map<String, String> plan = new LinkedHashMap<>();
        plan.put("monday", "ОФП + техника (60 мин)");
        plan.put("tuesday", "Техника бросков + бёрпи");
        plan.put("wednesday", "Восстановление / растяжка");
        plan.put("thursday", "Спарринг (контроль интенсивности)");
        plan.put("friday", "Бег 2000м + взрывная работа");
        plan.put("saturday", "Лёгкая техничка + анализ");
        plan.put("sunday", "Отдых");

        if (deficits.stream().anyMatch(d -> d.parameter().equals("pullups") && d.difference() < 0)) {
            plan.put("monday", "ОФП + подтягивания (5 подходов по максимуму)");
            plan.put("wednesday", "Подтягивания с отягощением 4x8");
        }
        if (deficits.stream().anyMatch(d -> d.parameter().equals("trainingsPerWeek") && d.difference() < 0)) {
            plan.put("tuesday", "Добавочная тренировка: техника 60 мин");
            plan.put("friday", "Добавочная: бег 2000м + ОФП");
        }
        if (deficits.stream().anyMatch(d -> d.parameter().equals("intensity") && d.difference() < 0)) {
            plan.put("thursday", "Спарринг с интенсивностью 7+ (контроль пульса)");
        }
        return plan;
    }

    private String generateTip(List<DeficitItemResponse> deficits) {
        return deficits.stream()
                .filter(d -> d.difference() < 0)
                .min((a, b) -> Double.compare(a.difference(), b.difference()))
                .map(d -> switch (d.parameter()) {
                    case "pullups" -> "Самый слабый параметр — подтягивания. Чемпионы делают больше.";
                    case "intensity" -> "Подними интенсивность: добавь спарринги и контроль темпа.";
                    case "trainingsPerWeek" -> "Добавь 1–2 короткие тренировки в неделю для стабильности.";
                    default -> "Держи фокус на слабых местах и прогрессируй постепенно.";
                })
                .orElse("Отличная работа. Поддерживай стабильность и восстановление.");
    }

    private static DeficitItemResponse diff(String p, double current, double target) {
        return new DeficitItemResponse(p, current, target, current - target);
    }

    private RecommendationResponse toResponse(Recommendation rec) {
        List<DeficitItemResponse> deficits = new ArrayList<>();
        if (rec.getDeficits() != null && !rec.getDeficits().isNull()) {
            deficits = objectMapper.convertValue(rec.getDeficits(), new TypeReference<>() {
            });
        }
        Map<String, String> weekPlan = new LinkedHashMap<>();
        if (rec.getWeekPlan() != null && !rec.getWeekPlan().isNull()) {
            weekPlan = objectMapper.convertValue(rec.getWeekPlan(), new TypeReference<>() {
            });
        }
        String custom = rec.getCustomTip();
        String tip = custom != null && !custom.isBlank() ? custom.trim() : generateTip(deficits);
        return new RecommendationResponse(rec.getGeneratedDate(), rec.getProgressPercent(), deficits, weekPlan, tip);
    }

    private static Map<String, String> normalizeWeekPlan(Map<String, String> weekPlan) {
        List<String> days = List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
        Map<String, String> out = new LinkedHashMap<>();
        for (String day : days) {
            String v = pickDayValue(weekPlan, day);
            out.put(day, v == null || v.isBlank() ? "—" : v.trim());
        }
        return out;
    }

    private static String pickDayValue(Map<String, String> m, String day) {
        if (m == null) {
            return null;
        }
        if (m.containsKey(day)) {
            return m.get(day);
        }
        String cap = day.substring(0, 1).toUpperCase(Locale.ROOT) + day.substring(1).toLowerCase(Locale.ROOT);
        if (m.containsKey(cap)) {
            return m.get(cap);
        }
        if (m.containsKey(day.toUpperCase(Locale.ROOT))) {
            return m.get(day.toUpperCase(Locale.ROOT));
        }
        return null;
    }

    private record Metrics(double trainingsPerWeek, double avgIntensity, int pullups) {
    }
}

