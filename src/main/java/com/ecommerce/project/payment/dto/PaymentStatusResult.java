package com.ecommerce.project.payment.dto;

import lombok.Builder;

// Provider-agnostic answer to "what happened to this payment?", used by reconciliation when a
// webhook never arrived. providerPaymentId is the id a refund must target - the same as the
// payment reference for Stripe (PaymentIntent id), but the captured payment's own id (pay_...)
// for Razorpay, whose payment reference is the Razorpay order id.
@Builder
public record PaymentStatusResult(
        State state,
        String providerPaymentId,
        Long amountMinorUnits,
        String currency
) {
    public enum State {
        // Not finished yet - includes "customer hasn't paid", "card declined but can retry", and
        // in-flight authentication. Never treated as a failure by reconciliation.
        PENDING,
        SUCCEEDED,
        CANCELLED
    }
}
