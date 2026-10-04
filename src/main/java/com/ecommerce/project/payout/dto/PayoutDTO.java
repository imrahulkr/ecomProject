package com.ecommerce.project.payout.dto;

import java.time.Instant;

public record PayoutDTO(
        Long id,
        Long sellerId,
        String sellerName,
        long amountMinorUnits,
        String currency,
        String reference,
        Instant createdAt
) {}
