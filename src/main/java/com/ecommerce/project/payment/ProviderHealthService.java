package com.ecommerce.project.payment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderHealthService {

    private final ProviderHealthRepository providerHealthRepository;

    @Value("${payment.provider.failure-threshold:3}")
    private int failureThreshold;

    @Value("${payment.provider.cooldown-minutes:5}")
    private long cooldownMinutes;

    @Transactional
    public void recordSuccess(ProviderName providerName) {
        ProviderHealth health = getOrCreate(providerName);
        health.setConsecutiveFailures(0);
        health.setCooldownUntil(null);
        providerHealthRepository.save(health);
    }

    @Transactional
    public void recordFailure(ProviderName providerName) {
        ProviderHealth health = getOrCreate(providerName);
        health.setConsecutiveFailures(health.getConsecutiveFailures() + 1);
        if (health.getConsecutiveFailures() >= failureThreshold) {
            health.setCooldownUntil(Instant.now().plus(cooldownMinutes, ChronoUnit.MINUTES));
        }
        providerHealthRepository.save(health);
    }

    public boolean isAvailable(ProviderName providerName) {
        return providerHealthRepository.findByProviderName(providerName)
                .map(h -> h.getCooldownUntil() == null || h.getCooldownUntil().isBefore(Instant.now()))
                .orElse(true);
    }

    // Available providers first (fewest consecutive failures), providers still in cooldown last.
    // Never makes a live network call - purely reads the locally tracked health state.
    public List<ProviderName> rankProviders(List<ProviderName> candidates) {
        Instant now = Instant.now();
        return candidates.stream()
                .sorted(Comparator
                        .comparing((ProviderName p) -> isInCooldown(p, now))
                        .thenComparingInt(this::getFailureCount))
                .toList();
    }

    private boolean isInCooldown(ProviderName providerName, Instant now) {
        return providerHealthRepository.findByProviderName(providerName)
                .map(h -> h.getCooldownUntil() != null && h.getCooldownUntil().isAfter(now))
                .orElse(false);
    }

    private int getFailureCount(ProviderName providerName) {
        return providerHealthRepository.findByProviderName(providerName)
                .map(ProviderHealth::getConsecutiveFailures)
                .orElse(0);
    }

    private ProviderHealth getOrCreate(ProviderName providerName) {
        return providerHealthRepository.findByProviderName(providerName)
                .orElseGet(() -> providerHealthRepository.save(new ProviderHealth(providerName)));
    }
}
