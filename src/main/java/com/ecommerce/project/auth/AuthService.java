package com.ecommerce.project.auth;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.dto.ForgotPasswordRequestDTO;
import com.ecommerce.project.auth.dto.PasswordChangeRequestDTO;
import com.ecommerce.project.auth.dto.UserResponse;
import com.ecommerce.project.security.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface AuthService {
    ResponseEntity<Map<String, String>> forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO);

    ResponseEntity<MessageResponse> validateEmailVerificationToken(String token);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    UserResponse getAllSellers(Pageable pageDetails);

    void saveVerificationTokenForUser(User user, String token);

    void changePassword(JwtPrincipal userDetails, PasswordChangeRequestDTO passwordChangeRequestDTO);
}
