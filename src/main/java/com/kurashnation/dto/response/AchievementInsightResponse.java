package com.kurashnation.dto.response;

import java.util.List;
import java.util.Map;

public record AchievementInsightResponse(
        String analysisText,
        Map<String, String> suggestedWeekPlan,
        List<WeekdayChartPointResponse> chartByWeekday,
        String aiSummary,
        Map<String, String> modelFocusByDay
) {
}
