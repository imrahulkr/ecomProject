package com.ecommerce.project.order;

import com.ecommerce.project.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT COALESCE( SUM(o.totalAmount), 0 ) FROM Order o")
    Double getTotalRevenue();
}
