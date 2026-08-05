package com.ecommerce.project.order;

import com.ecommerce.project.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT COALESCE( SUM(o.totalAmount), 0 ) FROM Order o")
    Double getTotalRevenue();

    Optional<Order> findByOrderIdAndEmail(Long orderId, String email);

    // DISTINCT: an order can have multiple line items belonging to the same seller, which the
    // join would otherwise duplicate into repeated rows for the same order.
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.user.userId = :sellerId")
    Page<Order> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
