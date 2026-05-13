package com.kurashnation.service.interfaces;

import com.kurashnation.dto.response.CoachOptionResponse;
import com.kurashnation.dto.response.CoachResponse;

import java.util.List;

public interface CoachService {
    CoachResponse myProfile(String email);

    List<CoachOptionResponse> listAllForAdmin();
}

