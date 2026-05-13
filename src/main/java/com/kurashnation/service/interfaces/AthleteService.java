package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.AthleteNameRequest;
import com.kurashnation.dto.request.UpsertAthleteRequest;
import com.kurashnation.dto.response.AthleteResponse;

import java.util.List;

public interface AthleteService {
    AthleteResponse getMyProfile(String email);

    AthleteResponse updateMyProfile(String email, UpsertAthleteRequest request);

    AthleteResponse patchMyDisplayName(String email, AthleteNameRequest request);

    AthleteResponse getAthleteById(String email, Long athleteId);

    List<AthleteResponse> getAthletesByCoach(String email, Long coachId);

    AthleteResponse adminAssignCoach(Long athleteId, Long coachId);
}

