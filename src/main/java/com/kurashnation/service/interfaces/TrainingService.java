package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.TrainingRequest;
import com.kurashnation.dto.response.TrainingResponse;

import java.util.List;
import java.util.Map;

public interface TrainingService {
    List<TrainingResponse> getMyTrainings(String email);

    TrainingResponse createTraining(String email, TrainingRequest request);

    TrainingResponse updateTraining(String email, Long trainingId, TrainingRequest request);

    void deleteTraining(String email, Long trainingId);

    Map<String, Object> weeklyStatistics(String email);
}

