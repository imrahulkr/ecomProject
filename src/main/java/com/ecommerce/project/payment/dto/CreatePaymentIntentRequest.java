package com.ecommerce.project.payment.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class CreatePaymentIntentRequest {
    String orderReference;
    long amountMinorUnits;
    String currency;
    String customerEmail;
    String customerName;
    String description;
    Map<String, String> metadata;
}
