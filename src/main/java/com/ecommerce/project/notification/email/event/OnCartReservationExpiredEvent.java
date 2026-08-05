package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.cart.Cart;
import org.springframework.context.ApplicationEvent;

public class OnCartReservationExpiredEvent extends ApplicationEvent {

    private final Cart cart;

    public OnCartReservationExpiredEvent(Object source, Cart cart) {
        super(source);
        this.cart = cart;
    }

    public Cart getCart() {
        return cart;
    }
}
