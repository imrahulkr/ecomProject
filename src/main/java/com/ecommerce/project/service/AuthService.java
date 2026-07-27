package com.ecommerce.project.service;

import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AuthenticationResult;
import com.ecommerce.project.payload.ForgotPasswordRequestDTO;
import com.ecommerce.project.payload.PasswordChangeRequestDTO;
import com.ecommerce.project.payload.UserResponse;
import com.ecommerce.project.security.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface AuthService {
    AuthenticationResult login(LoginRequest loginRequest);

    ResponseEntity<MessageResponse> register(SignupRequest signupRequest);

    ResponseEntity<Map<String, String>> forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO);

    ResponseEntity<MessageResponse> validateEmailVerificationToken(String token);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    ResponseCookie logout();

    UserResponse getAllSellers(Pageable pageDetails);

    void saveVerificationTokenForUser(User user, String token);

    void changePassword(JwtPrincipal userDetails, PasswordChangeRequestDTO passwordChangeRequestDTO);
}
