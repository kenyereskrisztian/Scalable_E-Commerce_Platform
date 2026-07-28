package com.ecommerce.userservice.service;

import com.ecommerce.userservice.domain.User;
import com.ecommerce.userservice.dto.UpdateUserRequest;

public interface UserService {
    User getById(Long id);
    User updateProfile(Long id, UpdateUserRequest request);
}
