package com.ecommerce.project.coupon.dto;

public record CouponApplicationResult(
        String couponCode,
        long subtotalMinorUnits,
        long discountMinorUnits,
        long finalPriceMinorUnits,
        String currency
) {}
