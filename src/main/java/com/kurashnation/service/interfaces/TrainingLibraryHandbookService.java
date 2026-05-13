package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.TrainingLibraryAnalyzeRequest;
import com.kurashnation.dto.request.TrainingLibraryApplyHandbookRequest;
import com.kurashnation.dto.response.HandbookWeekPlanLlm;
import com.kurashnation.dto.response.RecommendationResponse;
import com.kurashnation.dto.response.TrainingLibraryPreviewTextResponse;

public interface TrainingLibraryHandbookService {

    TrainingLibraryPreviewTextResponse previewText(Long fileId);

    HandbookWeekPlanLlm analyze(String coachEmail, Long fileId, TrainingLibraryAnalyzeRequest request);

    RecommendationResponse applyHandbook(String coachEmail, Long fileId, TrainingLibraryApplyHandbookRequest request);
}
