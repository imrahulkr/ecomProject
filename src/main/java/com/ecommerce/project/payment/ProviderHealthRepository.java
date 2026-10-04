package com.ecommerce.project.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderHealthRepository extends JpaRepository<ProviderHealth, Long> {
    Optional<ProviderHealth> findByProviderName(ProviderName providerName);
}
