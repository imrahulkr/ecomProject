package com.ecommerce.project.payment.event;

import com.ecommerce.project.payment.ProviderName;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PaymentEvent {
    ProviderName providerName;
    String providerEventId;
    PaymentEventType type;
    String providerPaymentReference;
    Long amountMinorUnits;
    String currency;
    Instant occurredAt;
    // Only populated for PAYMENT_FAILED - null otherwise.
    String failureReason;
}
