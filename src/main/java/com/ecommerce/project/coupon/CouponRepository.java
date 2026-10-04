package com.ecommerce.project.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    // Checkout takes this row lock before counting redemptions and holds it until the order
    // (and its redemption row) commits, so two concurrent checkouts can't both pass a limit
    // that only has room for one.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}
