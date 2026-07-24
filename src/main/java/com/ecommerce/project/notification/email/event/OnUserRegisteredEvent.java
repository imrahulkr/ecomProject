package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.model.User;
import org.springframework.context.ApplicationEvent;

public class OnUserRegisteredEvent extends ApplicationEvent {
    private final User savedUser;
    public OnUserRegisteredEvent(Object source, User savedUser) {
        super(source);
        this.savedUser = savedUser;
    }
    public User getUser() {
        return savedUser;
    }
}
