package com.ecommerce.project.coupon.dto;

import com.ecommerce.project.coupon.DiscountType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record CouponDTO(
        Long couponId,
        String code,
        String description,
        DiscountType discountType,
        Double discountPercentage,
        Long discountAmountMinorUnits,
        String currency,
        Long minOrderAmountMinorUnits,
        Integer maxRedemptions,
        int redemptionCount,
        Integer perUserLimit,
        Instant expiresAt,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
