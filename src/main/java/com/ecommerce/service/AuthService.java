package com.ecommerce.service;

import com.ecommerce.dto.outgoing.AuthResponse;
import com.ecommerce.dto.incoming.LoginRequest;
import com.ecommerce.dto.incoming.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}

