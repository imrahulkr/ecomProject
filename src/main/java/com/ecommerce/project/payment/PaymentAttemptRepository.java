package com.ecommerce.project.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByOrder_OrderIdOrderByCreatedAtDesc(Long orderId);

    List<PaymentAttempt> findByOrder_OrderIdAndStatusIn(Long orderId, Collection<PaymentAttemptStatus> statuses);

    List<PaymentAttempt> findByStatusInAndProviderPaymentReferenceIsNotNullAndCreatedAtBetween(
            Collection<PaymentAttemptStatus> statuses, Instant from, Instant to);

    List<PaymentAttempt> findByStatus(PaymentAttemptStatus status);

    // Most recent attempt wins when the same provider reference somehow appears twice (shouldn't
    // happen in practice - each createPaymentIntent call gets a fresh provider-side reference).
    Optional<PaymentAttempt> findFirstByProviderNameAndProviderPaymentReferenceOrderByCreatedAtDesc(
            ProviderName providerName, String providerPaymentReference);
}
