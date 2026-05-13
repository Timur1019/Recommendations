package com.kurashnation.service.interfaces;

import com.kurashnation.dto.response.UserResponse;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;

import java.util.List;

public interface UserService {
    User requireActiveUserByEmail(String email);

    List<UserResponse> getAllUsers();

    UserResponse updateRole(Long userId, UserRole role);

    UserResponse setActive(Long userId, boolean active);
}

