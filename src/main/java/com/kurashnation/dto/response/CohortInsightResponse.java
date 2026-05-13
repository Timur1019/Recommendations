package com.kurashnation.dto.response;

import java.util.List;
import java.util.Map;

public record CohortInsightResponse(
        String analysisText,
        Map<String, String> suggestedWeekPlan,
        List<WeekdayChartPointResponse> chartByWeekday,
        List<CohortAthleteBriefResponse> athletes,
        String aiSummary,
        Map<String, String> modelFocusByDay
) {
}
