package com.ecommerce.project.notification.email.event.listener;

import com.ecommerce.project.notification.email.event.OnOrderPaidEvent;
import com.ecommerce.project.notification.email.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OnOrderPaidListener {

    private final EmailService emailService;

    public OnOrderPaidListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void handleOrderPaid(OnOrderPaidEvent event) {
        emailService.sendOrderConfirmation(event.getOrder());
    }
}
