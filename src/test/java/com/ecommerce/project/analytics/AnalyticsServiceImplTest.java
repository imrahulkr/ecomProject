package com.ecommerce.project.analytics;

import com.ecommerce.project.analytics.dto.AnalyticsResponse;
import com.ecommerce.project.coupon.CouponRedemptionRepository;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.refund.RefundRepository;
import com.ecommerce.project.review.ReviewRepository;
import com.ecommerce.project.wishlist.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;
    @Mock
    private RefundRepository refundRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getAnalyticsData_aggregatesAllFeatureCounts() {
        when(productRepository.count()).thenReturn(42L);
        when(orderRepository.count()).thenReturn(7L);
        when(orderRepository.getRevenueMinorUnitsByStatus(OrderStatus.PAID.name())).thenReturn(600_00L);
        when(refundRepository.sumSucceeded()).thenReturn(100_00L);
        when(orderRepository.countByOrderStatus(OrderStatus.PAID.name())).thenReturn(4L);
        when(orderRepository.countByOrderStatus(OrderStatus.PENDING_PAYMENT.name())).thenReturn(1L);
        when(orderRepository.countByOrderStatus(OrderStatus.CANCELLED.name())).thenReturn(2L);
        when(reviewRepository.countByHiddenFalse()).thenReturn(15L);
        when(reviewRepository.getOverallAverageRating()).thenReturn(4.5);
        when(wishlistItemRepository.count()).thenReturn(9L);
        when(couponRedemptionRepository.countActiveRedemptions()).thenReturn(3L);
        when(couponRedemptionRepository.sumActiveDiscountMinorUnits()).thenReturn(1_50L);

        AnalyticsResponse response = analyticsService.getAnalyticsData();

        assertThat(response.productCount()).isEqualTo("42");
        assertThat(response.totalOrders()).isEqualTo("7");
        assertThat(response.totalRevenue()).isEqualTo("500.0");
        assertThat(response.reviewCount()).isEqualTo("15");
        assertThat(response.averageRating()).isEqualTo("4.5");
        assertThat(response.wishlistItemCount()).isEqualTo("9");
        assertThat(response.couponRedemptionCount()).isEqualTo("3");
        assertThat(response.couponDiscountTotal()).isEqualTo("1.5");
        assertThat(response.paidOrders()).isEqualTo("4");
        assertThat(response.pendingPaymentOrders()).isEqualTo("1");
        assertThat(response.cancelledOrders()).isEqualTo("2");
    }

    @Test
    void getAnalyticsData_defaultsAverageRatingToZeroWhenNoReviews() {
        when(productRepository.count()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.getRevenueMinorUnitsByStatus(OrderStatus.PAID.name())).thenReturn(null);
        when(reviewRepository.countByHiddenFalse()).thenReturn(0L);
        when(reviewRepository.getOverallAverageRating()).thenReturn(null);
        when(wishlistItemRepository.count()).thenReturn(0L);
        when(couponRedemptionRepository.countActiveRedemptions()).thenReturn(0L);
        when(couponRedemptionRepository.sumActiveDiscountMinorUnits()).thenReturn(0L);

        AnalyticsResponse response = analyticsService.getAnalyticsData();

        assertThat(response.averageRating()).isEqualTo("0.0");
        assertThat(response.totalRevenue()).isEqualTo("0.0");
    }
}
