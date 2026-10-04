package com.ecommerce.project.payout.dto;

// pending: earned on items still inside the return window (or with a return in progress).
// available: settled and not yet paid out - what the next payout will transfer.
public record EarningsSummaryDTO(
        String currency,
        double commissionPercent,
        int returnWindowDays,
        long pendingMinorUnits,
        long availableMinorUnits,
        long paidOutMinorUnits
) {}
