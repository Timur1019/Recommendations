package com.kurashnation.service.impl;

import com.kurashnation.dto.request.AiPromptSettingUpdateRequest;
import com.kurashnation.dto.response.AiPromptSettingResponse;
import com.kurashnation.exception.NotFoundException;
import com.kurashnation.model.entity.AiPromptSetting;
import com.kurashnation.repository.AiPromptSettingRepository;
import com.kurashnation.service.interfaces.AiPromptSettingService;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AiPromptSettingServiceImpl implements AiPromptSettingService {

    public static final String KEY_INSIGHT_WEEK_FOCUS_JSON = "INSIGHT_WEEK_FOCUS_JSON";

    public static final String KEY_HANDBOOK_WEEK_PLAN_JSON = "HANDBOOK_WEEK_PLAN_JSON";

    private final AiPromptSettingRepository aiPromptSettingRepository;

    public AiPromptSettingServiceImpl(AiPromptSettingRepository aiPromptSettingRepository) {
        this.aiPromptSettingRepository = aiPromptSettingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPromptSettingResponse> listAll() {
        return aiPromptSettingRepository.findAll().stream()
                .sorted(Comparator.comparing(AiPromptSetting::getSettingKey))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AiPromptSettingResponse update(String settingKey, AiPromptSettingUpdateRequest request) {
        AiPromptSetting row = aiPromptSettingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new NotFoundException("Prompt setting not found: " + settingKey));
        row.setLabel(request.label().trim());
        row.setBody(request.body());
        row = aiPromptSettingRepository.save(row);
        LogUtil.info("AI prompt updated key=%s", settingKey);
        return toDto(row);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveInsightWeekFocusSystemPrompt(String fallback) {
        return aiPromptSettingRepository.findBySettingKey(KEY_INSIGHT_WEEK_FOCUS_JSON)
                .map(AiPromptSetting::getBody)
                .filter(s -> s != null && !s.isBlank())
                .orElse(fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveHandbookWeekPlanSystemPrompt(String fallback) {
        return aiPromptSettingRepository.findBySettingKey(KEY_HANDBOOK_WEEK_PLAN_JSON)
                .map(AiPromptSetting::getBody)
                .filter(s -> s != null && !s.isBlank())
                .orElse(fallback);
    }

    private AiPromptSettingResponse toDto(AiPromptSetting e) {
        return new AiPromptSettingResponse(e.getSettingKey(), e.getLabel(), e.getBody(), e.getUpdatedAt());
    }
}
