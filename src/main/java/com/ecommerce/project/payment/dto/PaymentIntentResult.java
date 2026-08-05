package com.ecommerce.project.payment.dto;

import com.ecommerce.project.payment.ProviderName;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class PaymentIntentResult {
    ProviderName providerName;
    String providerReferenceId;
    String clientSecret;
    String status;
    Map<String, Object> clientPayload;
}
