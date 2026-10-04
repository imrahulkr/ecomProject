package com.ecommerce.project.analytics.dto;

public record AnalyticsResponse(
        String productCount,
        String totalRevenue,
        String totalOrders,
        String reviewCount,
        String averageRating,
        String wishlistItemCount,
        String couponRedemptionCount,
        String couponDiscountTotal,
        String paidOrders,
        String pendingPaymentOrders,
        String cancelledOrders
) {}
