package com.ecommerce.project.notification.email.outbox;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.repository.EmailLog;
import com.ecommerce.project.notification.email.repository.EmailLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Replaces the old @Async + @Retryable sender. Emails used to be handed to an in-memory thread
// pool straight from event listeners, which (a) sent them even when the triggering transaction
// later rolled back, (b) lost them on restart, and (c) threw TaskRejectedException back into the
// caller - e.g. payment handling - once the pool's queue filled up.
//
// enqueue() joins the caller's transaction, so the email row commits or rolls back together
// with whatever triggered it. EmailOutboxSender delivers due rows outside any transaction.
@Service
@RequiredArgsConstructor
public class EmailOutbox {

    private static final Logger logger = LoggerFactory.getLogger(EmailOutbox.class);

    private final EmailOutboxRepository repository;
    private final EmailLogRepository emailLogRepository;

    @Value("${app.email.retry.max-attempts}")
    private int maxAttempts;

    @Value("${app.email.retry.initial-delay-ms}")
    private long initialDelayMs;

    @Value("${app.email.retry.multiplier}")
    private double multiplier;

    @Transactional
    public void enqueue(EmailRequest request) {
        repository.save(EmailOutboxMessage.pending(request));
    }

    // Short transaction: lock due rows (skipping ones another instance holds), mark them
    // SENDING, return their ids. The network sends happen after this commits.
    @Transactional
    List<Long> claimDue(int batchSize, Duration staleAfter) {
        Instant now = Instant.now();
        List<EmailOutboxMessage> due = repository.findDueForUpdate(now, now.minus(staleAfter), PageRequest.of(0, batchSize));
        due.forEach(message -> message.setStatus(EmailOutboxStatus.SENDING));
        return due.stream().map(EmailOutboxMessage::getId).toList();
    }

    @Transactional
    void recordResult(Long id, EmailSendResult result) {
        EmailOutboxMessage message = repository.findById(id).orElse(null);
        if (message == null) {
            return;
        }
        message.setAttempts(message.getAttempts() + 1);

        if (result.isSuccess()) {
            message.setStatus(EmailOutboxStatus.SENT);
            message.setSentAt(Instant.now());
            message.setLastError(null);
            emailLogRepository.save(EmailLog.from(message.toRequest(), result));
            return;
        }

        message.setLastError(result.getErrorMessage());
        if (message.getAttempts() >= maxAttempts) {
            message.setStatus(EmailOutboxStatus.FAILED);
            emailLogRepository.save(EmailLog.from(message.toRequest(), result));
            logger.error("Email {} to {} permanently failed after {} attempts: {}",
                    id, message.getRecipient(), message.getAttempts(), result.getErrorMessage());
            return;
        }

        long delayMs = (long) (initialDelayMs * Math.pow(multiplier, message.getAttempts() - 1));
        message.setStatus(EmailOutboxStatus.PENDING);
        message.setNextAttemptAt(Instant.now().plusMillis(delayMs));
        logger.warn("Email {} to {} failed (attempt {}/{}), retrying in {} ms: {}",
                id, message.getRecipient(), message.getAttempts(), maxAttempts, delayMs, result.getErrorMessage());
    }
}
