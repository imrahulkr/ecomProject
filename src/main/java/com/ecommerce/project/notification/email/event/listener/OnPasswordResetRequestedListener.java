package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.notification.email.event.OnPasswordResetRequestedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import com.ecommerce.project.service.PasswordResetTokenService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnPasswordResetRequestedListener {

    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    public OnPasswordResetRequestedListener(PasswordResetTokenService passwordResetTokenService, EmailService emailService) {
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
    }

    @EventListener
    public void handlePasswordResetRequestedEvent(OnPasswordResetRequestedEvent event) {
        User user = event.getUser();

        String token = passwordResetTokenService.generateAndSaveToken(user);
        emailService.sendPasswordResetEmail(user, token);
    }
}
