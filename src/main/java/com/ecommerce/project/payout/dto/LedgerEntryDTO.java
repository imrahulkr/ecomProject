package com.ecommerce.project.payout.dto;

import com.ecommerce.project.payout.LedgerEntryType;

import java.time.Instant;

public record LedgerEntryDTO(
        Long id,
        LedgerEntryType entryType,
        long amountMinorUnits,
        String currency,
        Long orderId,
        Long orderItemId,
        boolean paidOut,
        Instant createdAt
) {}
