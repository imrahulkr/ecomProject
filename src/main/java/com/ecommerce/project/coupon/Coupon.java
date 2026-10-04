package com.ecommerce.project.coupon;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "coupons")
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long couponId;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    // Used when discountType == PERCENTAGE, 0-100. Null/unused otherwise.
    @Column(name = "discount_percentage")
    private Double discountPercentage;

    // Used when discountType == FIXED_AMOUNT, in minor units. Null/unused otherwise.
    @Column(name = "discount_amount_minor_units")
    private Long discountAmountMinorUnits;

    // Only meaningful (and required) for FIXED_AMOUNT coupons - the currency the fixed amount is in.
    @Column(length = 8)
    private String currency;

    @Column(name = "min_order_amount_minor_units")
    private Long minOrderAmountMinorUnits;

    // Null = unlimited.
    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    // Null = unlimited.
    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
