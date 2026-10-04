package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnSellerApplicationApprovedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnSellerApplicationApprovedListener {

    private final EmailService emailService;

    public OnSellerApplicationApprovedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleSellerApplicationApproved(OnSellerApplicationApprovedEvent event) {
        emailService.sendSellerApplicationApprovedEmail(event.getApplication());
    }
}
