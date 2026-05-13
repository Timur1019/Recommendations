package com.kurashnation.dto.response;

public record LoginResponse(
        String token,
        UserResponse user
) {
}

