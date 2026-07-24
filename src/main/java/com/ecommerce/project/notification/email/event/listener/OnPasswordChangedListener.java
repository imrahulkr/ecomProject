package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnPasswordChangedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class OnPasswordChangedListener {
    private final EmailService emailService;

    public OnPasswordChangedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleOnPasswordChangedListener(OnPasswordChangedEvent event){
        emailService.sendPasswordChangedEmail(event.getUser());
    }
}
