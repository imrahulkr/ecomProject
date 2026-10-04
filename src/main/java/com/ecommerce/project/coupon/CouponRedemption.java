package com.ecommerce.project.coupon;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.order.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

// One row per order that redeemed a coupon. There's no separate "confirmed" flag - whether a
// redemption counts against the coupon's maxRedemptions/perUserLimit is derived from the
// referenced order's live orderStatus (excluding CANCELLED/PAYMENT_FAILED) at query time, the same
// "recompute from live state" pattern the cart total and stock reservations already use, rather
// than trying to keep a cached status in sync via an event listener.
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "coupon_redemptions")
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "discount_minor_units", nullable = false)
    private long discountMinorUnits;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
