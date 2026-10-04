package com.ecommerce.project.payment;

public enum PaymentAttemptStatus {
    INITIATED,
    SUCCEEDED,
    FAILED,
    // Superseded by a retry, or its order was cancelled/expired. A provider-side success can
    // still arrive afterwards (cancel raced the customer paying) - reconciliation refunds it.
    CANCELLED,
    // Money was taken but the order can't use it (already paid by another attempt, or
    // cancelled). Stays here until the refund call succeeds; the reconciliation job retries it.
    REFUND_PENDING,
    REFUNDED
}
