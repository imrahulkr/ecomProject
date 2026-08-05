package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.order.Order;
import org.springframework.context.ApplicationEvent;

public class OnPaymentFailedEvent extends ApplicationEvent {

    private final Order order;
    private final String reason;

    public OnPaymentFailedEvent(Object source, Order order, String reason) {
        super(source);
        this.order = order;
        this.reason = reason;
    }

    public Order getOrder() {
        return order;
    }

    public String getReason() {
        return reason;
    }
}
