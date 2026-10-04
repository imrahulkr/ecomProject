package com.ecommerce.project.service;

import com.ecommerce.project.auth.PasswordResetToken;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.PasswordResetTokenRepository;
import com.ecommerce.project.auth.UserRepository;
import jakarta.validation.Valid;
import com.ecommerce.project.security.services.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService{

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public PasswordResetTokenServiceImpl(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService)
    {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public String generateAndSaveToken(User user) {
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);

        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(passwordResetToken);

        return token;
    }

    @Override
    public String validateToken(String token) {
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) return "INVALID";

        PasswordResetToken passwordResetToken = optionalToken.get();

        if(passwordResetToken.isUsed()) return "ALREADY_USED";
        if(passwordResetToken.isExpired()) return "EXPIRED";

        return "VALID";
    }

    // One transaction: consuming the token, setting the password and ending every existing
    // session (a reset usually means the account may be compromised) commit together.
    @Override
    @Transactional
    public String resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) return "INVALID";

        PasswordResetToken passwordResetToken = optionalToken.get();
        passwordResetTokenRepository.delete(passwordResetToken);
        if(passwordResetToken.isUsed()) return "ALREADY_USED";
        if(passwordResetToken.isExpired()) return "EXPIRED";

        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getUserId());

        return "SUCCESS";
    }
}
