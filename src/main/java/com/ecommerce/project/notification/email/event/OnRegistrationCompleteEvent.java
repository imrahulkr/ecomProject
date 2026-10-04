package com.ecommerce.project.notification.email.event;

import com.ecommerce.project.auth.User;
import org.springframework.context.ApplicationEvent;

public class OnRegistrationCompleteEvent extends ApplicationEvent {
    private final User savedUser;
    public OnRegistrationCompleteEvent(User savedUser) {
        super(savedUser);
        this.savedUser = savedUser;
    }
    public User getUser() {
        return savedUser;
    }
}
