package com.ecommerce.project.security.services;

import org.springframework.http.ResponseEntity;

import com.ecommerce.project.security.dto.AuthResponse;
import com.ecommerce.project.security.dto.LoginRequest;
import com.ecommerce.project.security.dto.MessageResponse;
import com.ecommerce.project.security.dto.SignupRequest;

public interface SecureAuthService {
    // public SecureAuthServiceImpl.IssuedTokens signup(SignupRequest request);
    public ResponseEntity<MessageResponse> signup(SignupRequest request);
    public SecureAuthServiceImpl.IssuedTokens login(LoginRequest request);
    public AuthResponse toAuthResponse(SecureAuthServiceImpl.IssuedTokens issuedTokens);
}
