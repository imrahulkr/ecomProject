package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnRefundProcessedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnRefundProcessedListener {

    private final EmailService emailService;

    public OnRefundProcessedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleRefundProcessed(OnRefundProcessedEvent event) {
        emailService.sendRefundProcessed(event.getOrderItem(), event.getAmountMinorUnits(), event.getReason());
    }
}
