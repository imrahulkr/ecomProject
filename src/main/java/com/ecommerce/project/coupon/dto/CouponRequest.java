package com.ecommerce.project.coupon.dto;

import com.ecommerce.project.coupon.DiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CouponRequest(
        @NotBlank String code,
        String description,
        @NotNull DiscountType discountType,
        @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
        @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
        Double discountPercentage,
        @Positive(message = "Discount amount must be greater than 0")
        Long discountAmountMinorUnits,
        String currency,
        @Positive(message = "Minimum order amount must be greater than 0")
        Long minOrderAmountMinorUnits,
        @Min(value = 1, message = "Max redemptions must be at least 1")
        Integer maxRedemptions,
        @Min(value = 1, message = "Per-user limit must be at least 1")
        Integer perUserLimit,
        Instant expiresAt,
        Boolean active
) {}
