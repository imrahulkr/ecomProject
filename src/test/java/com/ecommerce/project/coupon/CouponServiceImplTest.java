package com.ecommerce.project.coupon;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartItem;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private CouponServiceImpl couponService;

    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("buyer@example.com");

        cart = new Cart();
        cart.setUser(user);
        cart.setCurrency("INR");
        cart.setTotalPriceMinorUnits(10_000L);
        cart.setCartItems(List.of(new CartItem()));

        when(authUtil.loggedInUser()).thenReturn(user);
        when(cartRepository.findCartByEmail(user.getEmail())).thenReturn(cart);
    }

    private Coupon percentageCoupon(double pct) {
        Coupon coupon = new Coupon();
        coupon.setCouponId(1L);
        coupon.setCode("SAVE10");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountPercentage(pct);
        coupon.setActive(true);
        return coupon;
    }

    @Test
    void applyCouponToCart_computesPercentageDiscountAndAppliesToCart() {
        Coupon coupon = percentageCoupon(10.0);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        CouponApplicationResultAssertion.assertDiscount(
                couponService.applyCouponToCart("save10"), 10_000L, 1000L, 9000L, "INR");
        assertThat(cart.getAppliedCouponCode()).isEqualTo("SAVE10");
        assertThat(cart.getDiscountMinorUnits()).isEqualTo(1000L);
    }

    @Test
    void applyCouponToCart_capsFixedDiscountAtSubtotal() {
        Coupon coupon = new Coupon();
        coupon.setCouponId(2L);
        coupon.setCode("BIG");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountAmountMinorUnits(50_000L);
        coupon.setCurrency("INR");
        coupon.setActive(true);
        when(couponRepository.findByCode("BIG")).thenReturn(Optional.of(coupon));

        var result = couponService.applyCouponToCart("BIG");

        assertThat(result.discountMinorUnits()).isEqualTo(10_000L);
        assertThat(result.finalPriceMinorUnits()).isZero();
    }

    @Test
    void applyCouponToCart_rejectsExpiredCoupon() {
        Coupon coupon = percentageCoupon(10.0);
        coupon.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.applyCouponToCart("SAVE10"))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void applyCouponToCart_rejectsWhenBelowMinimumOrderAmount() {
        Coupon coupon = percentageCoupon(10.0);
        coupon.setMinOrderAmountMinorUnits(20_000L);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.applyCouponToCart("SAVE10"))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("minimum order amount");
    }

    @Test
    void applyCouponToCart_rejectsWhenRedemptionLimitReached() {
        Coupon coupon = percentageCoupon(10.0);
        coupon.setMaxRedemptions(5);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.countActiveRedemptionsForCoupon(1L)).thenReturn(5L);

        assertThatThrownBy(() -> couponService.applyCouponToCart("SAVE10"))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("redemption limit");
    }

    @Test
    void applyCouponToCart_rejectsUnknownCode() {
        when(couponRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.applyCouponToCart("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void applyCouponToCart_rejectsEmptyCart() {
        cart.setCartItems(List.of());

        assertThatThrownBy(() -> couponService.applyCouponToCart("SAVE10"))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Cart is empty");
    }

    // Small helper so the discount-shape assertion reads as one line above.
    private static final class CouponApplicationResultAssertion {
        static void assertDiscount(com.ecommerce.project.coupon.dto.CouponApplicationResult result,
                                    long subtotal, long discount, long finalPrice, String currency) {
            assertThat(result.subtotalMinorUnits()).isEqualTo(subtotal);
            assertThat(result.discountMinorUnits()).isEqualTo(discount);
            assertThat(result.finalPriceMinorUnits()).isEqualTo(finalPrice);
            assertThat(result.currency()).isEqualTo(currency);
        }
    }
}
