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
        AnalyticsResponse analyticsResponse = new AnalyticsResponse();
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();
        Double totalRevenue = orderRepository.getTotalRevenue();
        analyticsResponse.setProductCount(String.valueOf(productCount));
        analyticsResponse.setTotalOrders(String.valueOf(orderCount));
        analyticsResponse.setTotalRevenue(String.valueOf(totalRevenue != null ? totalRevenue : 0));
        return analyticsResponse;
    }
}
