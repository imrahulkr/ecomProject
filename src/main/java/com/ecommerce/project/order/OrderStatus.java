package com.ecommerce.project.order;

// Order.orderStatus stays a plain String column (pre-existing schema, holds free-text history
// like "Order Accepted !") - this enum is just the typed set of values new code writes/reads,
// via name()/valueOf(), without a migration to change the column's type.
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    // Not written by current code: a failed payment leaves the order PENDING_PAYMENT so the
    // customer can retry until the stock hold expires, and expiry moves it to CANCELLED. Kept
    // because existing rows/queries (coupon redemption counts) may still reference it.
    PAYMENT_FAILED,
    CANCELLED
}
