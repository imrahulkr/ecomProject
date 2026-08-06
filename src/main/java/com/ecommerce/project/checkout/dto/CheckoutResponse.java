package com.ecommerce.project.checkout.dto;

import com.ecommerce.project.payment.ProviderName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

// @Jacksonized (not just @Builder): this gets deserialized back out of storage on an idempotent
// replay (IdempotencyService stores it as JSON), and an immutable record has no setters for
// Jackson to use otherwise.
@Builder
@Jacksonized
public record CheckoutResponse(
        Long orderId,
        String orderStatus,
        ProviderName provider,
        long amountMinorUnits,
        String currency,

        // Populated when the payment intent was created successfully - frontend uses these to
        // complete payment client-side (Stripe.js / Razorpay Checkout).
        String clientSecret,
        Map<String, Object> clientPayload,

        // Populated instead, when the provider call itself failed - a clear "try again" signal
        // rather than a silent automatic retry. The order and its reservation remain in place.
        boolean paymentAttemptFailed,
        String failureReason
) {}
