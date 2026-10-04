package com.ecommerce.project.payout;

import com.ecommerce.project.order.FulfillmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SellerLedgerRepository extends JpaRepository<SellerLedgerEntry, Long> {

    Page<SellerLedgerEntry> findBySellerIdOrderByCreatedAtDescIdDesc(Long sellerId, Pageable pageable);

    List<SellerLedgerEntry> findByOrderItemIdAndEntryTypeIn(Long orderItemId, Collection<LedgerEntryType> types);

    @Query("SELECT COALESCE(SUM(e.amountMinorUnits), 0) FROM SellerLedgerEntry e WHERE e.sellerId = :sellerId AND e.payoutId IS NULL")
    long sumUnpaid(@Param("sellerId") Long sellerId);

    @Query("SELECT DISTINCT e.sellerId FROM SellerLedgerEntry e WHERE e.payoutId IS NULL")
    List<Long> findSellerIdsWithUnpaidEntries();

    // "Settled" entries: their item can no longer change the seller's earnings - cancelled,
    // returned, or delivered (return rejected) longer ago than the return window. Only these are
    // available to pay out; everything else is pending.
    String SETTLED_UNPAID = "FROM SellerLedgerEntry e, OrderItem oi WHERE oi.orderItemId = e.orderItemId "
            + "AND e.sellerId = :sellerId AND e.payoutId IS NULL "
            + "AND (oi.fulfillmentStatus IN :closed OR (oi.fulfillmentStatus IN :delivered AND oi.deliveredAt <= :cutoff))";

    @Query("SELECT COALESCE(SUM(e.amountMinorUnits), 0) " + SETTLED_UNPAID)
    long sumSettledUnpaid(@Param("sellerId") Long sellerId,
                          @Param("closed") Collection<FulfillmentStatus> closed,
                          @Param("delivered") Collection<FulfillmentStatus> delivered,
                          @Param("cutoff") LocalDateTime cutoff);

    // Locked so two concurrent payouts for the same seller can't both claim the same entries.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e " + SETTLED_UNPAID)
    List<SellerLedgerEntry> findSettledUnpaidForUpdate(@Param("sellerId") Long sellerId,
                                                       @Param("closed") Collection<FulfillmentStatus> closed,
                                                       @Param("delivered") Collection<FulfillmentStatus> delivered,
                                                       @Param("cutoff") LocalDateTime cutoff);
}
