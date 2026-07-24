package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.notification.email.model.EmailType;
import org.springframework.context.ApplicationEvent;

public class EmailTriggerEvent extends ApplicationEvent {
    private final EmailType type;
    private final Object payload; // User, Order, Cart etc.
    public EmailTriggerEvent(Object source, EmailType type, Object payload) {
        super(source);
        this.type = type;
        this.payload = payload;
    }
    public EmailType getType() {return type; }
    public Object getPayload() { return payload; }
}
