package com.kurashnation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record WeekDayScheduleRequest(
        @NotBlank String dayOfWeek,
        @NotEmpty @Valid List<TimeActivityRequest> entries
) {
}
