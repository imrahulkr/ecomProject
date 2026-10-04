package com.ecommerce.project.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDTO {
    private String paymentId;
    private String paymentMethod;
    private String pgPaymentId;
    private String pgStatus;
    private String pgResponseMessage;
    private String pgName;
    private Instant createdAt;
    private Instant updatedAt;
}
