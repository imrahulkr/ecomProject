package com.ecommerce.project.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);

    // Most recent attempt wins when the same provider reference somehow appears twice (shouldn't
    // happen in practice - each createPaymentIntent call gets a fresh provider-side reference).
    Optional<PaymentAttempt> findFirstByProviderNameAndProviderPaymentReferenceOrderByCreatedAtDesc(
            ProviderName providerName, String providerPaymentReference);
}
