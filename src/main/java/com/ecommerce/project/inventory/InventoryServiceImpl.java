package com.ecommerce.project.inventory;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.notification.email.event.OnCartReservationExpiredEvent;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.product.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ReservationExpiryTransactionHelper reservationExpiryTransactionHelper;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${inventory.reservation.ttl-minutes}")
    private int reservationTtlMinutes;

    @Override
    @Transactional
    public StockReservation reserve(Long productId, Integer quantity, Cart cart) {
        int updated = productRepository.decrementStockIfAvailable(productId, quantity);
        if (updated == 0) {
            throw new InsufficientStockException("Not enough stock available for product " + productId);
        }
        Product product = productRepository.getReferenceById(productId);
        Instant expiresAt = Instant.now().plus(reservationTtlMinutes, ChronoUnit.MINUTES);
        StockReservation reservation = new StockReservation(product, cart, quantity, expiresAt);
        return stockReservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void confirmReservation(Long reservationId) {
        int updated = stockReservationRepository.transitionIfActive(reservationId, ReservationStatus.CONFIRMED);
        if (updated == 0) {
            logger.warn("Reservation {} was not ACTIVE when confirming (already confirmed/released/expired)", reservationId);
        }
    }

    @Override
    @Transactional
    public void releaseReservation(Long reservationId) {
        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new InsufficientStockException("Reservation not found: " + reservationId));
        int updated = stockReservationRepository.transitionIfActive(reservationId, ReservationStatus.RELEASED);
        if (updated == 0) {
            // Already confirmed/released/expired by someone else - nothing to credit back.
            return;
        }
        productRepository.incrementStock(reservation.getProduct().getProductId(), reservation.getQuantity());
    }

    @Override
    @Transactional
    public void confirmReservationsForOrder(Long orderId) {
        List<StockReservation> reservations = stockReservationRepository
                .findByOrder_OrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (StockReservation reservation : reservations) {
            int updated = stockReservationRepository.transitionIfActive(reservation.getId(), ReservationStatus.CONFIRMED);
            if (updated == 0) {
                logger.warn("Reservation {} for order {} was not ACTIVE when confirming", reservation.getId(), orderId);
            }
        }
    }

    @Override
    @Transactional
    public void releaseReservationsForOrder(Long orderId) {
        List<StockReservation> reservations = stockReservationRepository
                .findByOrder_OrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (StockReservation reservation : reservations) {
            int updated = stockReservationRepository.transitionIfActive(reservation.getId(), ReservationStatus.RELEASED);
            if (updated == 0) {
                continue;
            }
            productRepository.incrementStock(reservation.getProduct().getProductId(), reservation.getQuantity());
        }
    }

    @Override
    public void expireDueReservations() {
        List<StockReservation> due = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, Instant.now());
        if (due.isEmpty()) {
            return;
        }
        // Keyed by cart id, not the Cart entity itself: Cart's Lombok-generated equals/hashCode
        // walks into cartItems -> CartItem.cart, which walks straight back into Cart - a Set<Cart>
        // would recurse forever the first time two reservations shared a cart.
        Map<Long, Cart> cartsToNotify = new LinkedHashMap<>();
        for (StockReservation reservation : due) {
            if (reservationExpiryTransactionHelper.expireIfActive(reservation)) {
                logger.info("Expired reservation id={}, productId={}, quantity={}, cartId={}",
                        reservation.getId(), reservation.getProduct().getProductId(),
                        reservation.getQuantity(), reservation.getCart().getCartId());
                cartsToNotify.put(reservation.getCart().getCartId(), reservation.getCart());
            }
        }
        cartsToNotify.values()
                .forEach(cart -> eventPublisher.publishEvent(new OnCartReservationExpiredEvent(this, cart)));
    }
}
