package com.kurashnation.dto.response;

import java.util.Map;

/**
 * Распарсенный ответ модели: краткое резюме и фокус по дням (ключи MONDAY…SUNDAY).
 */
public record LlmWeekFocus(String summary, Map<String, String> byDay) {

    public LlmWeekFocus {
        summary = summary == null ? "" : summary;
        byDay = byDay == null ? Map.of() : Map.copyOf(byDay);
    }
}
