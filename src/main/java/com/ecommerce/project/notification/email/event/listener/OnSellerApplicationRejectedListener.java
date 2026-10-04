package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnSellerApplicationRejectedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnSellerApplicationRejectedListener {

    private final EmailService emailService;

    public OnSellerApplicationRejectedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleSellerApplicationRejected(OnSellerApplicationRejectedEvent event) {
        emailService.sendSellerApplicationRejectedEmail(event.getApplication());
    }
}
