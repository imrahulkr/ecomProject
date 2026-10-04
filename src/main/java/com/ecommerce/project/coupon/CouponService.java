package com.ecommerce.project.coupon;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.coupon.dto.CouponApplicationResult;
import com.ecommerce.project.coupon.dto.CouponDTO;
import com.ecommerce.project.coupon.dto.CouponPageResponse;
import com.ecommerce.project.coupon.dto.CouponRequest;
import com.ecommerce.project.order.Order;

public interface CouponService {
    CouponApplicationResult applyCouponToCart(String code);

    void removeCouponFromCart();

    // Re-checks the cart's currently-applied coupon against live state (still active, not
    // expired, min order amount still met, redemption limits not exhausted) and recomputes the
    // discount from the cart's current subtotal. Returns null if no coupon is applied. Throws
    // APIException if a coupon is applied but no longer valid - the caller is expected to remove
    // it (or apply a different one) rather than have checkout silently change the price.
    AppliedCouponDiscount revalidateForCheckout(Cart cart);

    void recordRedemption(Coupon coupon, User user, Order order, long discountMinorUnits);

    CouponDTO createCoupon(CouponRequest request);

    CouponDTO updateCoupon(Long couponId, CouponRequest request);

    void deleteCoupon(Long couponId);

    CouponDTO getCoupon(Long couponId);

    CouponPageResponse getAllCoupons(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
