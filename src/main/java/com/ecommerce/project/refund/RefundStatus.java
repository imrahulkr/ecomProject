package com.ecommerce.project.refund;

public enum RefundStatus {
    // Recorded (item cancelled/returned, stock restocked) but the provider call hasn't succeeded
    // yet - also the state a crash between the two leaves behind; RefundService retries it.
    PENDING,
    SUCCEEDED,
    // Provider refused or was unreachable; retried each sweep up to RefundService.MAX_ATTEMPTS.
    FAILED
}
