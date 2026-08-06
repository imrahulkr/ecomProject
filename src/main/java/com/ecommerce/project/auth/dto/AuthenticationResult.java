package com.ecommerce.project.auth.dto;

import com.ecommerce.project.security.dto.UserInfoResponse;
import org.springframework.http.ResponseCookie;

public record AuthenticationResult(UserInfoResponse response, ResponseCookie jwtCookie) {}
