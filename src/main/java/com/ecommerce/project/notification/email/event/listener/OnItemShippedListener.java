package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnItemShippedEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnItemShippedListener {

    private final EmailService emailService;

    public OnItemShippedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleItemShipped(OnItemShippedEvent event) {
        emailService.sendShippingUpdate(event.getOrderItem());
    }
}
