package com.ecommerce.project.order;

import com.ecommerce.project.order.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
