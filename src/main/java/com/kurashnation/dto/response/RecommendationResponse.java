package com.kurashnation.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RecommendationResponse(
        LocalDate generatedDate,
        Integer progressPercent,
        List<DeficitItemResponse> deficits,
        Map<String, String> weekPlan,
        String tipOfTheDay
) {
}

