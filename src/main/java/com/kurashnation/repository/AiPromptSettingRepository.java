package com.kurashnation.repository;

import com.kurashnation.model.entity.AiPromptSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPromptSettingRepository extends JpaRepository<AiPromptSetting, Long> {
    Optional<AiPromptSetting> findBySettingKey(String settingKey);
}
