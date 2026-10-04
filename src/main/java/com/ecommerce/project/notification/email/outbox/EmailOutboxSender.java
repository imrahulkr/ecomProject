package com.ecommerce.project.notification.email.outbox;

import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

// Polls the outbox and delivers due emails. The provider call runs with no transaction open
// (claim and result recording are each their own short transaction in EmailOutbox).
@Component
@RequiredArgsConstructor
public class EmailOutboxSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailOutboxSender.class);
    // A row still SENDING after this long belongs to a sender that died mid-send - retry it.
    private static final Duration STALE_CLAIM = Duration.ofMinutes(5);

    private final EmailOutbox outbox;
    private final EmailOutboxRepository repository;
    private final EmailProvider emailProvider;

    @Value("${app.email.outbox.batch-size}")
    private int batchSize;

    @Scheduled(initialDelayString = "${app.email.outbox.poll-interval-ms}", fixedDelayString = "${app.email.outbox.poll-interval-ms}")
    public void sendDue() {
        try {
            List<Long> ids = outbox.claimDue(batchSize, STALE_CLAIM);
            ids.forEach(this::send);
        } catch (Exception e) {
            // Never let one bad sweep kill the scheduler thread - the next tick retries.
            logger.error("Email outbox sweep failed", e);
        }
    }

    private void send(Long id) {
        EmailOutboxMessage message = repository.findById(id).orElse(null);
        if (message == null) {
            return;
        }
        EmailSendResult result;
        try {
            result = emailProvider.sendEmail(message.toRequest());
        } catch (RuntimeException e) {
            result = EmailSendResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .providerUsed(emailProvider.getProviderName())
                    .build();
        }
        outbox.recordResult(id, result);
    }
}
