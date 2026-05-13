package com.kurashnation.dto.response;

import java.util.List;

public record DayScheduleResponse(String dayOfWeek, List<TimeActivityResponse> entries) {
}
