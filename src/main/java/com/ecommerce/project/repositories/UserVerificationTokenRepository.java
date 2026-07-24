package com.ecommerce.project.repositories;

import com.ecommerce.project.model.UserVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, Long> {
    Optional<UserVerificationToken> findByToken(String token);
}
