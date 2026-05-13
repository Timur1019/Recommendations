package com.kurashnation.service.interfaces;

import com.kurashnation.dto.response.RecommendationResponse;

import java.util.Map;
import java.util.Optional;

public interface RecommendationService {
    Optional<RecommendationResponse> latestForMe(String email);

    RecommendationResponse generateForMe(String email);

    RecommendationResponse generateForAthlete(String email, Long athleteId);

    Map<String, Object> compareWithGoldStandard(String email);

    Map<String, String> weekPlanForMe(String email);

    byte[] exportPdfForMe(String email);

    /** Сохранить недельный план и текст совета из справочника (тренер/админ). */
    RecommendationResponse saveHandbookPlanForAthlete(String coachEmail, Long athleteId, Map<String, String> weekPlan, String customTip);
}

