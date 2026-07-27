package com.ecommerce.service;

import com.ecommerce.domain.User;
import com.ecommerce.dto.incoming.UpdateUserRequest;

public interface UserService {
    User getById(Long id);
    User updateProfile(Long id, UpdateUserRequest request);
}
