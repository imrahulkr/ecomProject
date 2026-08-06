package com.ecommerce.project.analytics;

import com.ecommerce.project.analytics.dto.AnalyticsResponse;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService{

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    public AnalyticsResponse getAnalyticsData() {
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();
        Long totalRevenueMinorUnits = orderRepository.getTotalRevenueMinorUnits();
        double totalRevenue = (totalRevenueMinorUnits != null ? totalRevenueMinorUnits : 0) / 100.0;
        return new AnalyticsResponse(
                String.valueOf(productCount),
                String.valueOf(totalRevenue),
                String.valueOf(orderCount));
    }
}
