package com.ecommerce.project.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookDedupService {

    private final ProcessedWebhookEventRepository processedWebhookEventRepository;

    // Own transaction (REQUIRES_NEW): Postgres aborts the entire enclosing transaction on any
    // statement error, so a unique-constraint violation from this insert must not be allowed to
    // poison whatever transaction the caller is in - it needs its own commit/rollback boundary.
    //
    // Deliberately does NOT catch the constraint violation here: once a flush fails, Hibernate
    // marks that Session unusable for anything but rollback, even if the exception is caught -
    // catching it inside this method would leave Spring trying to commit a doomed Session. The
    // exception must propagate out so Spring rolls this transaction back cleanly; the caller
    // (not itself transactional) catches DataIntegrityViolationException afterwards instead.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(ProviderName providerName, String providerEventId) {
        processedWebhookEventRepository.saveAndFlush(new ProcessedWebhookEvent(providerName, providerEventId));
    }
}
