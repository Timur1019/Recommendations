package com.kurashnation.dto.response;

import java.util.List;

public record WeekdayChartPointResponse(String dayOfWeek, int activityCount, List<String> sampleActivities) {
}
