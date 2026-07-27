package com.ecommerce.project.repositories;

import com.ecommerce.project.model.OAuthAccount;
import com.ecommerce.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUserId (String provider, String providerUserId);
    List<OAuthAccount> findByUser(User user);
    boolean existsByUserAndProvider(User user, String provider);
    void deleteByUserAndProvider(User user, String provider);
}
