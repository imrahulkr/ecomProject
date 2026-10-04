package com.ecommerce.project.inventory;

import com.ecommerce.project.product.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Split out from InventoryServiceImpl solely so @Transactional actually applies per reservation:
// Spring's proxy-based AOP can't intercept a method calling another method on "this", so a loop
// calling a @Transactional method on itself would silently run with no transaction at all.
@Component
@RequiredArgsConstructor
class ReservationExpiryTransactionHelper {

    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;

    @Transactional
    boolean expireIfActive(StockReservation reservation) {
        int updated = stockReservationRepository.transitionIfActive(reservation.getId(), ReservationStatus.EXPIRED);
        if (updated == 0) {
            return false;
        }
        productRepository.incrementStock(reservation.getProduct().getProductId(), reservation.getQuantity());
        return true;
    }
}
