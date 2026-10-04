package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnItemDeliveredEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnItemDeliveredListener {

    private final EmailService emailService;

    public OnItemDeliveredListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleItemDelivered(OnItemDeliveredEvent event) {
        emailService.sendDeliveryConfirmation(event.getOrderItem());
    }
}
