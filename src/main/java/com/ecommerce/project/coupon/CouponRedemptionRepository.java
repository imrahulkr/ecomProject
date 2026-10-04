package com.ecommerce.project.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    // "Active" here excludes redemptions belonging to an order that never completed (CANCELLED)
    // or whose payment failed - see the CouponRedemption class comment for why this is computed
    // live from the order's status rather than stored on the redemption row itself.
    @Query("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.coupon.couponId = ?1 " +
            "AND cr.order.orderStatus NOT IN ('CANCELLED', 'PAYMENT_FAILED')")
    long countActiveRedemptionsForCoupon(Long couponId);

    @Query("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.coupon.couponId = ?1 AND cr.user.userId = ?2 " +
            "AND cr.order.orderStatus NOT IN ('CANCELLED', 'PAYMENT_FAILED')")
    long countActiveRedemptionsForCouponAndUser(Long couponId, Long userId);

    @Query("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.order.orderStatus NOT IN ('CANCELLED', 'PAYMENT_FAILED')")
    long countActiveRedemptions();

    @Query("SELECT COALESCE(SUM(cr.discountMinorUnits), 0) FROM CouponRedemption cr " +
            "WHERE cr.order.orderStatus NOT IN ('CANCELLED', 'PAYMENT_FAILED')")
    long sumActiveDiscountMinorUnits();
}
