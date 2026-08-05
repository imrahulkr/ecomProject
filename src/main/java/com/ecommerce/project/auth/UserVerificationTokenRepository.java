package com.ecommerce.project.auth;

import com.ecommerce.project.auth.UserVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.ecommerce.project.auth.UserVerificationTokenRepository;

@Repository
public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, Long> {
    Optional<UserVerificationToken> findByToken(String token);
}
