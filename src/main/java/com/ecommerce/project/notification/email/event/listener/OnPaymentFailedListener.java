package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnPaymentFailedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnPaymentFailedListener {

    private final EmailService emailService;

    public OnPaymentFailedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handlePaymentFailed(OnPaymentFailedEvent event) {
        emailService.sendPaymentFailedEmail(event.getOrder(), event.getReason());
    }
}
