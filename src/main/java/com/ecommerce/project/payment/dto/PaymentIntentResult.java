package com.ecommerce.project.payment.dto;

import com.ecommerce.project.payment.ProviderName;
import lombok.Builder;

import java.util.Map;

@Builder
public record PaymentIntentResult(
        ProviderName providerName,
        String providerReferenceId,
        String clientSecret,
        String status,
        Map<String, Object> clientPayload
) {}
