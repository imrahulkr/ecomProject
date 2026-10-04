package com.ecommerce.project.refund;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    // Everything already committed to refund on an order, whatever its delivery state - the
    // ceiling for the next refund is the order total minus this.
    @Query("SELECT COALESCE(SUM(r.amountMinorUnits), 0) FROM Refund r WHERE r.orderId = :orderId")
    long sumCommittedForOrder(@Param("orderId") Long orderId);

    @Query("SELECT COALESCE(SUM(r.amountMinorUnits), 0) FROM Refund r "
            + "WHERE r.status = com.ecommerce.project.refund.RefundStatus.SUCCEEDED")
    long sumSucceeded();

    List<Refund> findByStatusIn(List<RefundStatus> statuses);
}
