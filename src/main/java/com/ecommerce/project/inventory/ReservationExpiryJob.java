package com.ecommerce.project.inventory;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final InventoryService inventoryService;

    @Scheduled(
            initialDelayString = "${inventory.reservation.expiry-check-interval-ms}",
            fixedDelayString = "${inventory.reservation.expiry-check-interval-ms}"
    )
    public void run() {
        try {
            inventoryService.expireDueReservations();
        } catch (Exception e) {
            // Never let one bad sweep kill the scheduler thread - the next fixed-delay tick will retry.
            logger.error("Reservation expiry sweep failed", e);
        }
    }
}
