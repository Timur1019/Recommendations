package com.kurashnation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kurashnation.client.OpenAiResponsesClient;
import com.kurashnation.config.AiIntegrationProperties;
import com.kurashnation.dto.request.CohortInsightRequest;
import com.kurashnation.dto.response.AchievementInsightResponse;
import com.kurashnation.dto.response.CohortAthleteBriefResponse;
import com.kurashnation.dto.response.CohortInsightResponse;
import com.kurashnation.dto.response.LlmWeekFocus;
import com.kurashnation.dto.response.WeekdayChartPointResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Achievement;
import com.kurashnation.model.entity.AchievementTrainingWeek;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AchievementRepository;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.AchievementInsightService;
import com.kurashnation.service.interfaces.AiPromptSettingService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class AchievementInsightServiceImpl implements AchievementInsightService {

    private static final List<String> DAY_ORDER = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    /**
     * Модель должна вернуть только JSON — его парсим в {@link com.kurashnation.client.OpenAiResponsesClient#requestWeekFocus}.
     */
    private static final String LLM_JSON_SYSTEM_RU = """
            Ты помощник тренера по курашу. На входе — агрегированные данные журнала (дни, частые активности, черновик).
            Верни ТОЛЬКО один JSON-объект, без текста до и после, без markdown и без блоков ``` .
            Схема строго:
            {"summary":"2–5 предложений: общий вывод и ритм недели. Не медицинский совет.","byDay":{"MONDAY":"1–2 короткие фразы: фокус дня","TUESDAY":"...","WEDNESDAY":"...","THURSDAY":"...","FRIDAY":"...","SATURDAY":"...","SUNDAY":"..."}}
            Ключи byDay — только латиницей, как в примере. Пиши по-русски внутри строк. Если день без нагрузки — «отдых» или «лёгкая активность».
            """;

    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final CoachRepository coachRepository;
    private final AchievementRepository achievementRepository;
    private final AiIntegrationProperties aiIntegrationProperties;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final AiPromptSettingService aiPromptSettingService;
    private final AthleteProfileProvisioning athleteProfileProvisioning;
    private final CoachProfileProvisioning coachProfileProvisioning;

    public AchievementInsightServiceImpl(
            UserRepository userRepository,
            AthleteRepository athleteRepository,
            CoachRepository coachRepository,
            AchievementRepository achievementRepository,
            AiIntegrationProperties aiIntegrationProperties,
            OpenAiResponsesClient openAiResponsesClient,
            AiPromptSettingService aiPromptSettingService,
            AthleteProfileProvisioning athleteProfileProvisioning,
            CoachProfileProvisioning coachProfileProvisioning
    ) {
        this.userRepository = userRepository;
        this.athleteRepository = athleteRepository;
        this.coachRepository = coachRepository;
        this.achievementRepository = achievementRepository;
        this.aiIntegrationProperties = aiIntegrationProperties;
        this.openAiResponsesClient = openAiResponsesClient;
        this.aiPromptSettingService = aiPromptSettingService;
        this.athleteProfileProvisioning = athleteProfileProvisioning;
        this.coachProfileProvisioning = coachProfileProvisioning;
    }

    @Override
    @Transactional(readOnly = true)
    public AchievementInsightResponse insightForAthlete(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.ATHLETE) throw new ValidationException("Access denied");
        Athlete athlete = athleteProfileProvisioning.ensureAthleteProfile(user);
        List<Achievement> list = achievementRepository.findAllByAthleteIdWithWeeksOrderByCompetitionDateDesc(athlete.getId());
        return buildPersonalInsight(list, user.getFullName());
    }

    @Override
    @Transactional(readOnly = true)
    public CohortInsightResponse insightForCoach(String email, CohortInsightRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != UserRole.COACH) throw new ValidationException("Access denied");
        Coach coach = coachProfileProvisioning.ensureCoachProfile(user);

        List<Long> athleteIds;
        if (request.athleteIds() == null || request.athleteIds().isEmpty()) {
            athleteIds = athleteRepository.findAllByCoachId(coach.getId()).stream().map(Athlete::getId).toList();
        } else {
            athleteIds = request.athleteIds();
            for (Long aid : athleteIds) {
                Athlete a = athleteRepository.findByIdFetchUserAndCoach(aid)
                        .orElseThrow(() -> new NotFoundException("Athlete not found"));
                if (a.getCoach() == null || !coach.getId().equals(a.getCoach().getId())) {
                    throw new ValidationException("Access denied for athlete " + aid);
                }
            }
        }

        if (athleteIds.isEmpty()) {
            return new CohortInsightResponse(
                    "В группе пока нет спортсменов с привязкой к вам.",
                    Map.of(),
                    List.of(),
                    List.of(),
                    "",
                    Map.of()
            );
        }

        List<Achievement> all = athleteIds.isEmpty() ? List.of() : achievementRepository.findAllByAthleteIdInWithWeeks(athleteIds);
        Agg agg = aggregate(all);

        List<CohortAthleteBriefResponse> brief = new ArrayList<>();
        for (Long aid : athleteIds) {
            Athlete at = athleteRepository.findByIdFetchUserAndCoach(aid).orElse(null);
            if (at == null) continue;
            long ach = all.stream().filter(x -> x.getAthlete().getId().equals(aid)).count();
            long wks = all.stream().filter(x -> x.getAthlete().getId().equals(aid))
                    .mapToLong(x -> x.getTrainingWeeks().size()).sum();
            brief.add(new CohortAthleteBriefResponse(
                    aid,
                    at.getUser().getFullName(),
                    (int) ach,
                    (int) wks
            ));
        }

        String analysis = buildAnalysisText(agg, all.size(), brief.size(), true);
        Optional<LlmWeekFocus> weekFocus = fetchWeekFocusIfPossible(agg, all.size(), brief.size(), true, brief);
        String aiSummary = weekFocus.map(LlmWeekFocus::summary).orElse("");
        Map<String, String> modelFocus = weekFocus.map(LlmWeekFocus::byDay).orElse(Collections.emptyMap());
        if (!aiSummary.isBlank()) {
            analysis = analysis + "\n\n" + aiSummary;
        }
        if (weekFocus.isEmpty() && aiIntegrationProperties.isConfigured() && agg.totalWeeks > 0) {
            LogUtil.warn("Insight (когорта): JSON-план модели недоступен — см. логи OpenAI выше");
        }
        return new CohortInsightResponse(analysis, agg.suggestedPlan, agg.chart, brief, aiSummary, modelFocus);
    }

    private AchievementInsightResponse buildPersonalInsight(List<Achievement> achievements, String fullName) {
        Agg agg = aggregate(achievements);
        String analysis = buildAnalysisText(agg, achievements.size(), 1, false);
        analysis = "Спортсмен: " + fullName + ". " + analysis;
        Optional<LlmWeekFocus> weekFocus = fetchWeekFocusIfPossible(agg, achievements.size(), 1, false, List.of());
        String aiSummary = weekFocus.map(LlmWeekFocus::summary).orElse("");
        Map<String, String> modelFocus = weekFocus.map(LlmWeekFocus::byDay).orElse(Collections.emptyMap());
        if (!aiSummary.isBlank()) {
            analysis = analysis + "\n\n" + aiSummary;
        }
        if (weekFocus.isEmpty() && aiIntegrationProperties.isConfigured() && agg.totalWeeks > 0) {
            LogUtil.warn("Insight: JSON-план модели недоступен — см. логи OpenAI выше");
        }
        return new AchievementInsightResponse(analysis, agg.suggestedPlan, agg.chart, aiSummary, modelFocus);
    }

    private Optional<LlmWeekFocus> fetchWeekFocusIfPossible(
            Agg agg,
            int achievementCount,
            int peopleCount,
            boolean cohort,
            List<CohortAthleteBriefResponse> brief
    ) {
        if (!aiIntegrationProperties.isConfigured()) {
            LogUtil.info("Insight: LLM не вызывается — нет ключа API (app.ai)");
            return Optional.empty();
        }
        if (agg.totalWeeks == 0) {
            LogUtil.info("Insight: LLM не вызывается — нет недель с расписанием (totalWeeks=0)");
            return Optional.empty();
        }
        String payload = buildInsightPayload(agg, achievementCount, peopleCount, cohort, brief);
        LogUtil.info(
                "Insight: запрос LLM (JSON-план) cohort=%s achievements=%d people=%d weeks=%d",
                cohort,
                achievementCount,
                peopleCount,
                agg.totalWeeks
        );
        String systemPrompt = aiPromptSettingService.resolveInsightWeekFocusSystemPrompt(LLM_JSON_SYSTEM_RU);
        return openAiResponsesClient.requestWeekFocus(systemPrompt, payload);
    }

    private String buildInsightPayload(
            Agg agg,
            int achievementCount,
            int peopleCount,
            boolean cohort,
            List<CohortAthleteBriefResponse> brief
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(cohort ? "Режим: сводка по группе тренера.\n" : "Режим: индивидуальная сводка.\n");
        sb.append("Достижений учтено: ").append(achievementCount).append(".\n");
        sb.append("Недель с расписанием: ").append(agg.totalWeeks).append(".\n");
        if (cohort) {
            sb.append("Спортсменов в выборке: ").append(peopleCount).append(".\n");
        }
        sb.append("Активность по дням (число слотов): ").append(agg.dayEntryTotals).append("\n");
        sb.append("Черновик плана (частые активности по дням):\n").append(agg.suggestedPlan).append("\n");
        if (brief != null && !brief.isEmpty()) {
            sb.append("\nСпортсмены (имя, достижений, недель расписания):\n");
            for (CohortAthleteBriefResponse b : brief) {
                sb.append("- ")
                        .append(b.fullName())
                        .append(": достижений ")
                        .append(b.achievementCount())
                        .append(", недель ")
                        .append(b.trainingWeekCount())
                        .append("\n");
            }
        }
        sb.append("\nСформируй ответ строго в формате JSON из системной инструкции (summary + byDay).");
        return sb.toString();
    }

    private String buildAnalysisText(Agg agg, int achievementCount, int athleteCount, boolean cohort) {
        if (agg.totalWeeks == 0) {
            return cohort
                    ? "Нет сохранённых недельных расписаний у выбранных спортсменов. Добавьте достижения с детализацией недель."
                    : "Добавьте достижения и укажите хотя бы одну неделю с расписанием (время и активность по дням) — тогда здесь появится ИИ-сводка и черновик недельного плана.";
        }
        String busiest = agg.busiestDay();
        String prefix = cohort
                ? String.format(Locale.ROOT, "Сводка по %d спортсменам, %d достижений, %d недель расписания. ", athleteCount, achievementCount, agg.totalWeeks)
                : String.format(Locale.ROOT, "Учтено %d достижений и %d недель с расписанием. ", achievementCount, agg.totalWeeks);
        return prefix + "Наиболее заполненный день недели: " + busiest
                + ". Рекомендуемый ритм ниже собран из наиболее частых паттернов — эвристика, не медицинский совет.";
    }

    private Agg aggregate(List<Achievement> achievements) {
        Map<String, Integer> slotCounts = new HashMap<>();
        Map<String, Integer> dayEntryTotals = new HashMap<>();

        int totalWeeks = 0;
        for (Achievement a : achievements) {
            for (AchievementTrainingWeek w : a.getTrainingWeeks()) {
                totalWeeks++;
                accumulateSchedule(w.getScheduleJson(), slotCounts, dayEntryTotals);
            }
        }

        Map<String, String> plan = new LinkedHashMap<>();
        List<WeekdayChartPointResponse> chart = new ArrayList<>();
        for (String day : DAY_ORDER) {
            int cnt = dayEntryTotals.getOrDefault(day, 0);
            Map<String, Integer> labelScore = new HashMap<>();
            String prefix = day + "|";
            for (Map.Entry<String, Integer> e : slotCounts.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    String label = e.getKey().substring(prefix.length());
                    labelScore.merge(label, e.getValue(), Integer::sum);
                }
            }
            List<String> top = labelScore.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .toList();
            plan.put(day, String.join("; ", top));
            chart.add(new WeekdayChartPointResponse(day, cnt, top));
        }
        return new Agg(totalWeeks, dayEntryTotals, plan, chart);
    }

    private void accumulateSchedule(JsonNode json, Map<String, Integer> slotCounts, Map<String, Integer> dayEntryTotals) {
        if (json == null || !json.has("days") || !json.get("days").isArray()) {
            return;
        }
        for (JsonNode day : json.get("days")) {
            String dow = day.path("dayOfWeek").asText("").toUpperCase(Locale.ROOT);
            if (dow.isEmpty()) continue;
            JsonNode entArr = day.get("entries");
            if (entArr == null || !entArr.isArray()) continue;
            for (JsonNode e : entArr) {
                String time = e.path("time").asText("").trim();
                String act = e.path("activity").asText("").trim();
                if (act.isEmpty()) continue;
                String label = time.isEmpty() ? act : time + " — " + act;
                String key = dow + "|" + label;
                slotCounts.merge(key, 1, Integer::sum);
                dayEntryTotals.merge(dow, 1, Integer::sum);
            }
        }
    }

    private static final class Agg {
        final int totalWeeks;
        final Map<String, Integer> dayEntryTotals;
        final Map<String, String> suggestedPlan;
        final List<WeekdayChartPointResponse> chart;

        Agg(int totalWeeks, Map<String, Integer> dayEntryTotals,
            Map<String, String> suggestedPlan, List<WeekdayChartPointResponse> chart) {
            this.totalWeeks = totalWeeks;
            this.dayEntryTotals = dayEntryTotals;
            this.suggestedPlan = suggestedPlan;
            this.chart = chart;
        }

        String busiestDay() {
            return dayEntryTotals.entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse("—");
        }
    }
}
