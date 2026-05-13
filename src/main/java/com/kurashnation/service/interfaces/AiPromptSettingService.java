package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.AiPromptSettingUpdateRequest;
import com.kurashnation.dto.response.AiPromptSettingResponse;

import java.util.List;

public interface AiPromptSettingService {
    List<AiPromptSettingResponse> listAll();

    AiPromptSettingResponse update(String settingKey, AiPromptSettingUpdateRequest request);

    /** Текст системной инструкции для недельного JSON-фокуса (Insight); если в БД пусто — вернуть {@code fallback}. */
    String resolveInsightWeekFocusSystemPrompt(String fallback);

    /** Системная инструкция для JSON-плана из документа справочника. */
    String resolveHandbookWeekPlanSystemPrompt(String fallback);
}
