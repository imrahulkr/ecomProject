package com.ecommerce.project.coupon;

// Result of revalidating a cart's applied coupon at checkout time - carries the Coupon entity
// through so CheckoutTransactionExecutor can hand it back to CouponService.recordRedemption
// once the order is actually persisted, all inside the same transaction.
public record AppliedCouponDiscount(Coupon coupon, long discountMinorUnits) {}
