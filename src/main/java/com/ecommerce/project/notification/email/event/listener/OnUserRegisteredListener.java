package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnUserRegisteredEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import com.ecommerce.project.service.AuthService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OnUserRegisteredListener {
    private final EmailService emailService;
    private final AuthService authService;

    public OnUserRegisteredListener(EmailService emailService, AuthService authService) {
        this.emailService = emailService;
        this.authService = authService;
    }

    @EventListener
    public void handleUserRegistered(OnUserRegisteredEvent event){
        String token = UUID.randomUUID().toString();
        authService.saveVerificationTokenForUser(event.getUser(), token);
        emailService.sendVerificationEmail(event.getUser(), token);
    }

}
