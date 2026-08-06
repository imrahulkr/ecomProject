package com.ecommerce.project.payment.dto;

import lombok.Builder;

@Builder
public record RefundRequest(String providerPaymentId, Long amountMinorUnits, String reason) {}
