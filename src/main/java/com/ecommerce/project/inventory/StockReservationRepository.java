package com.ecommerce.project.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    List<StockReservation> findByOrder_OrderIdAndStatus(Long orderId, ReservationStatus status);

    // Conditional guard on the reservation's own status, mirroring the product-stock update below:
    // only the caller that wins this UPDATE (status still ACTIVE) is allowed to release the stock,
    // so a scheduled expiry sweep racing a manual release/confirm can't credit quantity back twice.
    @Modifying
    @Query("UPDATE StockReservation r SET r.status = :newStatus WHERE r.id = :id AND r.status = com.ecommerce.project.inventory.ReservationStatus.ACTIVE")
    int transitionIfActive(Long id, ReservationStatus newStatus);
}
