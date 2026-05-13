package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.SubmitTrainingRequestNoteDto;

public interface TrainingRequestSubmissionService {

    long submitForAthlete(String email, SubmitTrainingRequestNoteDto dto);
}
