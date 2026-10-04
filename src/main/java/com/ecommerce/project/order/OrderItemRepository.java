package com.ecommerce.project.order;

import com.ecommerce.project.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByOrderItemIdAndSellerId(Long orderItemId, Long sellerId);

    // "Verified purchase": the user has a paid order containing the product that wasn't cancelled.
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.product.productId = :productId "
            + "AND oi.order.email = :email AND oi.order.orderStatus = 'PAID' "
            + "AND oi.fulfillmentStatus <> com.ecommerce.project.order.FulfillmentStatus.CANCELLED")
    boolean hasPurchased(@Param("email") String email, @Param("productId") Long productId);
}
