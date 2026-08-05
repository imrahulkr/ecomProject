package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.order.OrderItem;
import org.springframework.context.ApplicationEvent;

public class OnItemDeliveredEvent extends ApplicationEvent {

    private final OrderItem orderItem;

    public OnItemDeliveredEvent(Object source, OrderItem orderItem) {
        super(source);
        this.orderItem = orderItem;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }
}
