package com.ecommerce.project.order;

public enum FulfillmentStatus {
    PENDING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    // Returns: customer asks (within app.returns.window-days of delivery), then the seller/admin
    // approves (RETURNED - refunded and restocked) or rejects (RETURN_REJECTED).
    RETURN_REQUESTED,
    RETURNED,
    RETURN_REJECTED
}
