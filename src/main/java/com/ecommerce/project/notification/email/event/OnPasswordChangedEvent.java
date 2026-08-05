package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.auth.User;
import org.springframework.context.ApplicationEvent;

public class OnPasswordChangedEvent extends ApplicationEvent {

    private final User user;

    public OnPasswordChangedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
