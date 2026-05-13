package com.kurashnation.service.impl;

import com.kurashnation.client.OpenAiResponsesClient;
import com.kurashnation.dto.request.TrainingLibraryAnalyzeRequest;
import com.kurashnation.dto.request.TrainingLibraryApplyHandbookRequest;
import com.kurashnation.dto.response.HandbookWeekPlanLlm;
import com.kurashnation.dto.response.RecommendationResponse;
import com.kurashnation.dto.response.TrainingLibraryPreviewTextResponse;
import com.kurashnation.dto.response.TrainingLibraryResolvedFile;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.TrainingLibraryFile;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.repository.TrainingLibraryFileRepository;
import com.kurashnation.repository.UserRepository;
import com.kurashnation.service.interfaces.AiPromptSettingService;
import com.kurashnation.service.interfaces.RecommendationService;
import com.kurashnation.service.interfaces.TrainingLibraryHandbookService;
import com.kurashnation.service.interfaces.TrainingLibraryService;
import com.kurashnation.util.LogUtil;
import com.kurashnation.util.TrainingLibraryTextExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingLibraryHandbookServiceImpl implements TrainingLibraryHandbookService {

    private static final int PREVIEW_MAX_CHARS = 200_000;
    private static final int LLM_MAX_CHARS = 48_000;

    private static final String FALLBACK_HANDBOOK_SYSTEM = """
            Ты помощник тренера по курашу. На входе — текст документа справочника и краткий контекст спортсмена (если есть).
            Составь вывод и недельный план (7 дней). Не давай медицинских диагнозов.
            Верни ТОЛЬКО один JSON-объект без markdown: {"summary":"...","weekPlan":{"monday":"...","tuesday":"...","wednesday":"...","thursday":"...","friday":"...","saturday":"...","sunday":"..."}}
            Ключи weekPlan — строчные латиницей. Текст внутри — по-русски.""";

    private final TrainingLibraryService trainingLibraryService;
    private final TrainingLibraryFileRepository trainingLibraryFileRepository;
    private final OpenAiResponsesClient openAiResponsesClient;
    private final AiPromptSettingService aiPromptSettingService;
    private final UserRepository userRepository;
    private final CoachRepository coachRepository;
    private final AthleteRepository athleteRepository;
    private final RecommendationService recommendationService;

    public TrainingLibraryHandbookServiceImpl(
            TrainingLibraryService trainingLibraryService,
            TrainingLibraryFileRepository trainingLibraryFileRepository,
            OpenAiResponsesClient openAiResponsesClient,
            AiPromptSettingService aiPromptSettingService,
            UserRepository userRepository,
            CoachRepository coachRepository,
            AthleteRepository athleteRepository,
            RecommendationService recommendationService
    ) {
        this.trainingLibraryService = trainingLibraryService;
        this.trainingLibraryFileRepository = trainingLibraryFileRepository;
        this.openAiResponsesClient = openAiResponsesClient;
        this.aiPromptSettingService = aiPromptSettingService;
        this.userRepository = userRepository;
        this.coachRepository = coachRepository;
        this.athleteRepository = athleteRepository;
        this.recommendationService = recommendationService;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingLibraryPreviewTextResponse previewText(Long fileId) {
        TrainingLibraryResolvedFile f = trainingLibraryService.resolveStoredFile(fileId);
        String full = TrainingLibraryTextExtractor.extract(f.path(), f.contentType(), f.originalFilename());
        if (full.isEmpty()) {
            throw new ValidationException("В файле не удалось извлечь текст для просмотра");
        }
        boolean truncated = full.length() > PREVIEW_MAX_CHARS;
        String text = truncated ? full.substring(0, PREVIEW_MAX_CHARS) : full;
        return new TrainingLibraryPreviewTextResponse(text, truncated);
    }

    @Override
    public HandbookWeekPlanLlm analyze(String coachEmail, Long fileId, TrainingLibraryAnalyzeRequest request) {
        User requester = userRepository.findByEmail(coachEmail).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.COACH)) {
            throw new ValidationException("Access denied");
        }
        TrainingLibraryAnalyzeRequest req = request == null ? new TrainingLibraryAnalyzeRequest(null, null) : request;

        TrainingLibraryFile meta = trainingLibraryFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));
        TrainingLibraryResolvedFile f = trainingLibraryService.resolveStoredFile(fileId);
        String docText = TrainingLibraryTextExtractor.extract(f.path(), f.contentType(), f.originalFilename());
        if (docText.isEmpty()) {
            throw new ValidationException("В документе нет извлекаемого текста для анализа");
        }
        if (docText.length() > LLM_MAX_CHARS) {
            docText = docText.substring(0, LLM_MAX_CHARS);
        }

        String athleteBlock = "";
        if (req.athleteId() != null) {
            Athlete athlete = athleteRepository.findByIdFetchUserAndCoach(req.athleteId())
                    .orElseThrow(() -> new NotFoundException("Athlete not found"));
            assertCoachOrAdminForAthlete(requester, athlete);
            StringBuilder sb = new StringBuilder();
            sb.append("Спортсмен: id=").append(athlete.getId()).append('\n');
            sb.append("Весовая категория: ").append(athlete.getWeightCategory()).append('\n');
            if (athlete.getGoalText() != null && !athlete.getGoalText().isBlank()) {
                sb.append("Цель: ").append(athlete.getGoalText().trim()).append('\n');
            }
            if (athlete.getRank() != null && !athlete.getRank().isBlank()) {
                sb.append("Разряд: ").append(athlete.getRank().trim()).append('\n');
            }
            athleteBlock = sb.toString();
        }

        String notes = req.coachNotes() == null ? "" : req.coachNotes().trim();
        String userPayload = """
                Документ: %s
                Файл: %s
                ---
                Текст:
                %s
                ---
                Контекст спортсмена:
                %s
                Комментарий тренера:
                %s
                """.formatted(
                meta.getTitle(),
                meta.getOriginalFilename(),
                docText,
                athleteBlock.isEmpty() ? "(не указан)" : athleteBlock,
                notes.isEmpty() ? "(нет)" : notes
        );

        String instructions = aiPromptSettingService.resolveHandbookWeekPlanSystemPrompt(FALLBACK_HANDBOOK_SYSTEM);
        HandbookWeekPlanLlm result = openAiResponsesClient.requestHandbookWeekPlan(instructions, userPayload)
                .orElseThrow(() -> new ValidationException(
                        "ИИ не вернул корректный план. Проверьте ключ API в настройках и повторите попытку."));
        LogUtil.info("Handbook AI analyzed fileId=%s coach=%s", fileId, coachEmail);
        return result;
    }

    @Override
    @Transactional
    public RecommendationResponse applyHandbook(String coachEmail, Long fileId, TrainingLibraryApplyHandbookRequest request) {
        User requester = userRepository.findByEmail(coachEmail).orElseThrow(() -> new NotFoundException("User not found"));
        if (!(requester.getRole() == UserRole.ADMIN || requester.getRole() == UserRole.COACH)) {
            throw new ValidationException("Access denied");
        }
        TrainingLibraryFile meta = trainingLibraryFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));
        Athlete athlete = athleteRepository.findByIdFetchUserAndCoach(request.athleteId())
                .orElseThrow(() -> new NotFoundException("Athlete not found"));
        assertCoachOrAdminForAthlete(requester, athlete);

        String customTip = request.summary().trim()
                + "\n\n— План по справочнику: «" + meta.getTitle() + "» (" + meta.getOriginalFilename() + ")";
        return recommendationService.saveHandbookPlanForAthlete(coachEmail, request.athleteId(), request.weekPlan(), customTip);
    }

    private void assertCoachOrAdminForAthlete(User requester, Athlete athlete) {
        if (requester.getRole() == UserRole.ADMIN) {
            return;
        }
        Coach coach = coachRepository.findByUserId(requester.getId())
                .orElseThrow(() -> new NotFoundException("Coach profile not found"));
        if (athlete.getCoach() == null || !athlete.getCoach().getId().equals(coach.getId())) {
            throw new ValidationException("Можно работать только со своими спортсменами");
        }
    }
}
