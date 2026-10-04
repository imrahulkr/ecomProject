package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.order.OrderItem;
import org.springframework.context.ApplicationEvent;

public class OnRefundProcessedEvent extends ApplicationEvent {

    private final OrderItem orderItem;
    private final long amountMinorUnits;
    private final String reason;

    public OnRefundProcessedEvent(Object source, OrderItem orderItem, long amountMinorUnits, String reason) {
        super(source);
        this.orderItem = orderItem;
        this.amountMinorUnits = amountMinorUnits;
        this.reason = reason;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public String getReason() {
        return reason;
    }
}
