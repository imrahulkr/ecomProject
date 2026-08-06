package com.ecommerce.project.payment.dto;

import com.ecommerce.project.payment.ProviderName;
import lombok.Builder;

@Builder
public record RefundResult(ProviderName providerName, String providerRefundId, String status, Long amountMinorUnits) {}
