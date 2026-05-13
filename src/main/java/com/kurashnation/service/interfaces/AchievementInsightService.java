package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.CohortInsightRequest;
import com.kurashnation.dto.response.AchievementInsightResponse;
import com.kurashnation.dto.response.CohortInsightResponse;

public interface AchievementInsightService {
    AchievementInsightResponse insightForAthlete(String email);

    CohortInsightResponse insightForCoach(String email, CohortInsightRequest request);
}
