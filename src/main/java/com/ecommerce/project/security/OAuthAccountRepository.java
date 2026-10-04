package com.ecommerce.project.security;

import com.ecommerce.project.security.OAuthAccount;
import com.ecommerce.project.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.ecommerce.project.security.OAuthAccountRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUserId (String provider, String providerUserId);
    List<OAuthAccount> findByUser(User user);
    boolean existsByUserAndProvider(User user, String provider);
    void deleteByUserAndProvider(User user, String provider);
}
