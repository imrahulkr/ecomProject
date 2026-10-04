package com.ecommerce.project.payout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerPayoutRepository extends JpaRepository<SellerPayout, Long> {

    Page<SellerPayout> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<SellerPayout> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amountMinorUnits), 0) FROM SellerPayout p WHERE p.sellerId = :sellerId")
    long sumForSeller(@Param("sellerId") Long sellerId);
}
