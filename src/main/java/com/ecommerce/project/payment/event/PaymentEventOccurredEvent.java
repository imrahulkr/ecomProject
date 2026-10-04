package com.ecommerce.project.payment.event;

import org.springframework.context.ApplicationEvent;

// Spring ApplicationEvent wrapper around a normalized PaymentEvent, published only after dedup
// passes. Keeps the payment package itself unaware of orders/checkout - anything that cares what
// a payment outcome means for an order listens for this instead of the payment package reaching
// into order/checkout/inventory directly.
public class PaymentEventOccurredEvent extends ApplicationEvent {

    private final PaymentEvent paymentEvent;

    public PaymentEventOccurredEvent(Object source, PaymentEvent paymentEvent) {
        super(source);
        this.paymentEvent = paymentEvent;
    }

    public PaymentEvent getPaymentEvent() {
        return paymentEvent;
    }
}
