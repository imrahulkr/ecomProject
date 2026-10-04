package com.ecommerce.project.notification.email.outbox;

public enum EmailOutboxStatus {
    PENDING,
    // Claimed by a sender. Reclaimed if it stays here too long (the sender died mid-send).
    SENDING,
    SENT,
    // Gave up after app.email.retry.max-attempts.
    FAILED
}
