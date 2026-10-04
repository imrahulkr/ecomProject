package com.ecommerce.project.order;

import com.ecommerce.project.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // SELECT ... FOR UPDATE: serializes everything that moves an order between payment states
    // (webhook, reconciliation poller, customer cancel, hold expiry) - see OrderPaymentTransitions.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    // Orders still awaiting payment whose stock hold has lapsed (or that never had one, e.g.
    // rows from before expiry cancelled the order itself).
    @Query("SELECT o.orderId FROM Order o WHERE o.orderStatus = 'PENDING_PAYMENT' AND NOT EXISTS ("
            + "SELECT r.id FROM StockReservation r WHERE r.order = o "
            + "AND r.status = com.ecommerce.project.inventory.ReservationStatus.ACTIVE AND r.expiresAt > :now)")
    List<Long> findPendingPaymentOrderIdsWithoutLiveHold(@Param("now") Instant now);

    // Revenue counts only orders in the given status (PAID) - summing every order would include
    // unpaid and cancelled ones.
    @Query("SELECT COALESCE( SUM(o.amountMinorUnits), 0 ) FROM Order o WHERE o.orderStatus = :status")
    Long getRevenueMinorUnitsByStatus(@Param("status") String status);

    long countByOrderStatus(String orderStatus);

    Page<Order> findByOrderStatus(String orderStatus, Pageable pageable);

    Optional<Order> findByOrderIdAndEmail(Long orderId, String email);

    Page<Order> findByEmail(String email, Pageable pageable);

    // DISTINCT: an order can have multiple line items belonging to the same seller, which the
    // join would otherwise duplicate into repeated rows for the same order.
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.user.userId = :sellerId")
    Page<Order> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
