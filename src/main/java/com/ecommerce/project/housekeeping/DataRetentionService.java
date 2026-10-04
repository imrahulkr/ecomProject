package com.ecommerce.project.housekeeping;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Prunes rows that only matter for a limited time and would otherwise grow forever. Each window
// is chosen to outlive the last moment the row can still be used:
//  - refresh tokens: once expired they can't be rotated, and reuse detection only matters for
//    tokens that could still be presented;
//  - idempotency records: clients retry within minutes, not weeks;
//  - processed webhook ids: providers stop redelivering within days (Stripe ~3, Razorpay 1);
//  - reset/verification tokens: kept a while past expiry so an old link says "expired", not "invalid";
//  - delivered outbox emails: email_logs keeps the audit trail (pruned after 180 days).
@Service
public class DataRetentionService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Map<String, Integer> purgeExpired() {
        Instant now = Instant.now();
        LocalDateTime localNow = LocalDateTime.now();
        Map<String, Integer> deleted = new LinkedHashMap<>();

        deleted.put("refresh_token", delete(
                "DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff", now.minus(Duration.ofDays(1))));
        deleted.put("idempotency_records", delete(
                "DELETE FROM IdempotencyRecord r WHERE r.createdAt < :cutoff", now.minus(Duration.ofDays(7))));
        deleted.put("processed_webhook_event", delete(
                "DELETE FROM ProcessedWebhookEvent e WHERE e.processedAt < :cutoff", now.minus(Duration.ofDays(30))));
        deleted.put("password_reset_tokens", delete(
                "DELETE FROM PasswordResetToken t WHERE t.expirationDate < :cutoff", localNow.minusDays(1)));
        deleted.put("verification_token", delete(
                "DELETE FROM UserVerificationToken t WHERE t.expiryDate < :cutoff", localNow.minusDays(7)));
        deleted.put("email_outbox", delete(
                "DELETE FROM EmailOutboxMessage m WHERE m.status = com.ecommerce.project.notification.email.outbox.EmailOutboxStatus.SENT "
                        + "AND m.sentAt < :cutoff", now.minus(Duration.ofDays(30))));
        deleted.put("email_logs", delete(
                "DELETE FROM EmailLog l WHERE l.createdAt < :cutoff", now.minus(Duration.ofDays(180))));
        return deleted;
    }

    private int delete(String jpql, Object cutoff) {
        return entityManager.createQuery(jpql).setParameter("cutoff", cutoff).executeUpdate();
    }
}
