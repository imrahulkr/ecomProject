package com.ecommerce.project.service;

import com.ecommerce.project.model.PasswordResetToken;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.PasswordResetTokenRepository;
import com.ecommerce.project.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService{

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetTokenServiceImpl(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,PasswordEncoder passwordEncoder)
    {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String generateAndSaveToken(User user) {
//        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetToken ->
//            passwordResetTokenRepository.delete(passwordResetToken)
//        );

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

    @Override
    public String resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> optionalToken = passwordResetTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) return "INVALID";

        PasswordResetToken passwordResetToken = optionalToken.get();
        passwordResetTokenRepository.delete(passwordResetToken);
        if(passwordResetToken.isUsed()) return "ALREADY USED";
        if(passwordResetToken.isExpired()) return "EXPIRED";

        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        //passwordResetTokenRepository.delete(passwordResetToken);

        return "SUCCESS";
    }
}
