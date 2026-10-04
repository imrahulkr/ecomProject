package com.ecommerce.project.auth;

import com.ecommerce.project.auth.PasswordResetToken;
import com.ecommerce.project.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.ecommerce.project.auth.PasswordResetTokenRepository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
    void deleteByUser(User user);
}
