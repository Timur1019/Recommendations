package com.kurashnation.dto.response;

import java.util.Map;

public record HandbookWeekPlanLlm(String summary, Map<String, String> weekPlan) {
}
