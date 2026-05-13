package com.kurashnation.service.interfaces;

import com.kurashnation.dto.request.LoginRequest;
import com.kurashnation.dto.request.RegisterAthleteRequest;
import com.kurashnation.dto.request.RegisterCoachRequest;
import com.kurashnation.dto.request.UserProfileUpdateRequest;
import com.kurashnation.dto.response.LoginResponse;
import com.kurashnation.dto.response.UserResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    UserResponse registerAthlete(RegisterAthleteRequest request);

    UserResponse registerCoach(RegisterCoachRequest request);

    UserResponse updateMyUserProfile(String email, UserProfileUpdateRequest request);
}

