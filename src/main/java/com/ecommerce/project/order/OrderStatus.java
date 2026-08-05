package com.ecommerce.project.order;

// Order.orderStatus stays a plain String column (pre-existing schema, holds free-text history
// like "Order Accepted !") - this enum is just the typed set of values new code writes/reads,
// via name()/valueOf(), without a migration to change the column's type.
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED
}
