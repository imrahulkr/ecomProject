package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.notification.email.event.OnCartReservationExpiredEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OnCartReservationExpiredListener {

    private final EmailService emailService;
    private final CartRepository cartRepository;

    public OnCartReservationExpiredListener(EmailService emailService, CartRepository cartRepository) {
        this.emailService = emailService;
        this.cartRepository = cartRepository;
    }

    // The publishing sweep (InventoryServiceImpl.expireDueReservations) is deliberately not
    // transactional - it only wraps each reservation's own state change, so the Cart on this
    // event is already detached by the time this synchronous listener runs. Re-fetching by id
    // here gives sendAbandonedCartReminder a managed Cart so its lazy cartItems can load.
    @EventListener
    @Transactional(readOnly = true)
    public void handleCartReservationExpired(OnCartReservationExpiredEvent event) {
        Cart cart = cartRepository.findById(event.getCart().getCartId()).orElse(null);
        if (cart == null || cart.getUser() == null) {
            return;
        }
        emailService.sendAbandonedCartReminder(cart.getUser(), cart);
    }
}
