package com.ecommerce.project.payout.dto;

public record SellerBalanceDTO(
        Long sellerId,
        String sellerName,
        String email,
        String currency,
        long pendingMinorUnits,
        long availableMinorUnits,
        long paidOutMinorUnits
) {}
