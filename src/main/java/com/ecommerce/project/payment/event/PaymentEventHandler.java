package com.ecommerce.project.payment.event;

import com.ecommerce.project.payment.WebhookDedupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventHandler.class);

    private final WebhookDedupService webhookDedupService;
    private final ApplicationEventPublisher eventPublisher;

    public void handle(PaymentEvent event) {
        try {
            webhookDedupService.markProcessed(event.getProviderName(), event.getProviderEventId());
        } catch (DataIntegrityViolationException e) {
            logger.info("Ignoring duplicate webhook event: provider={}, eventId={}",
                    event.getProviderName(), event.getProviderEventId());
            return;
        }
        logger.info("Processed payment event: type={}, provider={}, reference={}, amount={} {}",
                event.getType(), event.getProviderName(), event.getProviderPaymentReference(),
                event.getAmountMinorUnits(), event.getCurrency());
        eventPublisher.publishEvent(new PaymentEventOccurredEvent(this, event));
    }
}
