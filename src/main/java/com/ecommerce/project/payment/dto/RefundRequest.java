package com.ecommerce.project.payment.dto;

import lombok.Builder;

@Builder
// idempotencyKey: same value on every retry of the same refund, so a retry after a success we
// failed to record can't refund twice (honoured by Stripe; Razorpay's API has no equivalent).
public record RefundRequest(String providerPaymentId, Long amountMinorUnits, String reason, String idempotencyKey) {}
