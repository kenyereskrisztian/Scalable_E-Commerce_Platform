package com.ecommerce.service;

import com.ecommerce.domain.User;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;

public interface AuthService {
    User register(RegisterRequest request);
    User login(LoginRequest request);
}

