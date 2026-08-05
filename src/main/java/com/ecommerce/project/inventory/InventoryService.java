package com.ecommerce.project.inventory;

import com.ecommerce.project.cart.Cart;

public interface InventoryService {

    /**
     * Atomically claims stock for a product and records the claim as a time-boxed reservation.
     * Participates in the caller's transaction (no propagation override) so that, from checkout,
     * the stock claim and the resulting order row commit or roll back together as one unit.
     */
    StockReservation reserve(Long productId, Integer quantity, Cart cart);

    /**
     * Marks a reservation as permanently consumed (e.g. payment succeeded). Does not touch
     * product stock - it was already deducted at reserve() time.
     */
    void confirmReservation(Long reservationId);

    /**
     * Releases a still-active reservation and credits the stock back. Safe to call more than
     * once or concurrently with the expiry sweep - only the first caller to flip the reservation
     * out of ACTIVE actually credits stock back.
     */
    void releaseReservation(Long reservationId);

    /**
     * Confirms every still-active reservation attached to an order (payment succeeded).
     */
    void confirmReservationsForOrder(Long orderId);

    /**
     * Releases every still-active reservation attached to an order, crediting stock back
     * (order permanently abandoned, as opposed to a payment attempt merely failing while the
     * order stays open for a retry).
     */
    void releaseReservationsForOrder(Long orderId);

    /**
     * Scheduled entry point: releases every reservation whose hold has expired without payment
     * completing, and notifies each affected cart's owner once.
     */
    void expireDueReservations();
}
