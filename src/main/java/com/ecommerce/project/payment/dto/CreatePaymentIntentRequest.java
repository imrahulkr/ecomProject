package com.ecommerce.project.payment.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record CreatePaymentIntentRequest(
        String orderReference,
        long amountMinorUnits,
        String currency,
        String customerEmail,
        String customerName,
        String description,
        Map<String, String> metadata
) {}
