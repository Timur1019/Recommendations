package com.kurashnation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AchievementWeekResponse(Long id, LocalDate weekStartDate, List<DayScheduleResponse> days) {
}
