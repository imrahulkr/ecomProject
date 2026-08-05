package com.ecommerce.project.checkout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKeyAndEndpoint(
            Long userId, String idempotencyKey, IdempotencyEndpoint endpoint);

    // Only the caller that wins this conditional flip gets to redo the work; a losing racer
    // falls back to reporting a conflict instead of running the checkout twice.
    @Modifying
    @Query("UPDATE IdempotencyRecord r SET r.status = com.ecommerce.project.checkout.IdempotencyStatus.IN_PROGRESS " +
            "WHERE r.id = :id AND r.status = com.ecommerce.project.checkout.IdempotencyStatus.FAILED")
    int reclaimIfFailed(Long id);
}
