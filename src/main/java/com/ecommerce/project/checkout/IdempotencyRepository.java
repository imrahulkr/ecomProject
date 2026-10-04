package com.ecommerce.project.checkout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKeyAndEndpoint(
            Long userId, String idempotencyKey, IdempotencyEndpoint endpoint);

    // Only the caller that wins this conditional flip gets to redo the work; a losing racer
    // falls back to reporting a conflict instead of running the checkout twice.
    // @Transactional: IdempotencyService deliberately calls these outside a transaction, and
    // custom @Modifying queries (unlike the built-in CRUD methods) don't open one themselves.
    @Transactional
    @Modifying
    @Query("UPDATE IdempotencyRecord r SET r.status = com.ecommerce.project.checkout.IdempotencyStatus.IN_PROGRESS " +
            "WHERE r.id = :id AND r.status = com.ecommerce.project.checkout.IdempotencyStatus.FAILED")
    int reclaimIfFailed(Long id);

    // A claim stuck IN_PROGRESS (the server died mid-request) is handed to the next caller once
    // it's older than the cutoff; otherwise that Idempotency-Key would answer 409 forever.
    @Transactional
    @Modifying
    @Query("UPDATE IdempotencyRecord r SET r.updatedAt = :now " +
            "WHERE r.id = :id AND r.status = com.ecommerce.project.checkout.IdempotencyStatus.IN_PROGRESS AND r.updatedAt < :cutoff")
    int reclaimIfStale(@Param("id") Long id, @Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
