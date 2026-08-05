package com.ecommerce.project.payment.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefundRequest {
    String providerPaymentId;
    Long amountMinorUnits;
    String reason;
}
