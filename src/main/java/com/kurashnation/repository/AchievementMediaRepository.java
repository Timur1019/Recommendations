package com.kurashnation.repository;

import com.kurashnation.model.entity.AchievementMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementMediaRepository extends JpaRepository<AchievementMedia, Long> {
    List<AchievementMedia> findByAchievement_IdOrderByCreatedAtDesc(Long achievementId);

    List<AchievementMedia> findByAchievement_Id(Long achievementId);

    void deleteByAchievement_Id(Long achievementId);
}
