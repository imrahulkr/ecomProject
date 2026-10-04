package com.ecommerce.project.analytics;

import com.ecommerce.project.refund.RefundRepository;
import com.ecommerce.project.analytics.dto.AnalyticsResponse;
import com.ecommerce.project.coupon.CouponRedemptionRepository;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.review.ReviewRepository;
import com.ecommerce.project.wishlist.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService{

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final RefundRepository refundRepository;

    @Override
    public AnalyticsResponse getAnalyticsData() {
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();
        long paidOrders = orderRepository.countByOrderStatus(OrderStatus.PAID.name());
        long pendingPaymentOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING_PAYMENT.name());
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED.name());
        Long paidMinorUnits = orderRepository.getRevenueMinorUnitsByStatus(OrderStatus.PAID.name());
        // Net of item refunds (cancellations/returns) - money that came back out isn't revenue.
        long netRevenueMinorUnits = (paidMinorUnits != null ? paidMinorUnits : 0) - refundRepository.sumSucceeded();
        double totalRevenue = netRevenueMinorUnits / 100.0;

        long reviewCount = reviewRepository.countByHiddenFalse();
        Double averageRating = reviewRepository.getOverallAverageRating();

        long wishlistItemCount = wishlistItemRepository.count();

        long couponRedemptionCount = couponRedemptionRepository.countActiveRedemptions();
        double couponDiscountTotal = couponRedemptionRepository.sumActiveDiscountMinorUnits() / 100.0;

        return new AnalyticsResponse(
                String.valueOf(productCount),
                String.valueOf(totalRevenue),
                String.valueOf(orderCount),
                String.valueOf(reviewCount),
                String.valueOf(averageRating != null ? averageRating : 0.0),
                String.valueOf(wishlistItemCount),
                String.valueOf(couponRedemptionCount),
                String.valueOf(couponDiscountTotal),
                String.valueOf(paidOrders),
                String.valueOf(pendingPaymentOrders),
                String.valueOf(cancelledOrders));
    }
}
