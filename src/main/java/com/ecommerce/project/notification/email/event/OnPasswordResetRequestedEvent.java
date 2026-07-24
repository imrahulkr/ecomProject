package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.model.User;
import org.springframework.context.ApplicationEvent;

public class OnPasswordResetRequestedEvent extends ApplicationEvent {

    private final User user;

    public OnPasswordResetRequestedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
