package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.order.Order;
import org.springframework.context.ApplicationEvent;

public class OnOrderPaidEvent extends ApplicationEvent {

    private final Order order;

    public OnOrderPaidEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
