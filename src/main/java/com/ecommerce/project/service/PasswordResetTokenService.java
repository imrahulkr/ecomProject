package com.ecommerce.project.service;

import com.ecommerce.project.auth.User;

public interface PasswordResetTokenService {

    String generateAndSaveToken(User user);
    String validateToken(String token);
    String resetPassword(String token, String newPassword);
}
