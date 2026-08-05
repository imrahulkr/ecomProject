package com.ecommerce.project.order;

import com.ecommerce.project.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByOrderItemIdAndSellerId(Long orderItemId, Long sellerId);
}
