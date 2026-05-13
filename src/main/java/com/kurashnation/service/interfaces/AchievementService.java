package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.AchievementBatchRequest;
import com.kurashnation.dto.request.AchievementRequest;
import com.kurashnation.dto.response.AchievementResponse;

import java.util.List;

public interface AchievementService {
    List<AchievementResponse> myAchievements(String email);

    AchievementResponse requestAddAchievement(String email, AchievementRequest request);

    List<AchievementResponse> requestBatchAchievements(String email, AchievementBatchRequest request);

    AchievementResponse updateAchievement(String email, Long id, AchievementRequest request);

    void deleteAchievement(String email, Long id);

    AchievementResponse verifyByAdmin(String email, Long id);
}
