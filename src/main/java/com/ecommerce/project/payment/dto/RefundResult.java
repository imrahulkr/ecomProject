package com.ecommerce.project.payment.dto;

import com.ecommerce.project.payment.ProviderName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefundResult {
    ProviderName providerName;
    String providerRefundId;
    String status;
    Long amountMinorUnits;
}
