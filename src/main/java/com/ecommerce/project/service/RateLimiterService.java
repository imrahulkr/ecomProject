package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// In-memory, per-instance token buckets. Buckets live in a bounded Caffeine cache that evicts
// idle entries, so a flood of distinct IPs/emails can't grow memory without limit (the previous
// ConcurrentHashMap never forgot a key). Running several instances behind a load balancer gives
// each its own counters - swap the cache for Redis (bucket4j-redis) if that ever matters.
//
// Client IPs come from request.getRemoteAddr(): behind a reverse proxy set
// server.forward-headers-strategy=native so Tomcat resolves the real client from
// X-Forwarded-For (it only trusts that header from internal proxy addresses).
@Component
public class RateLimiterService {

    public enum Policy {
        // Login attempts from one IP, successful or not - slows credential stuffing across accounts.
        LOGIN_IP(20, Duration.ofMinutes(15)),
        // Failed logins against one account - locks that account's password login for a while.
        LOGIN_FAILURES_PER_ACCOUNT(5, Duration.ofMinutes(15)),
        SIGNUP_IP(5, Duration.ofHours(1)),
        PASSWORD_RESET_IP(3, Duration.ofMinutes(15)),
        PASSWORD_RESET_EMAIL(3, Duration.ofHours(1));

        private final int capacity;
        private final Duration window;

        Policy(int capacity, Duration window) {
            this.capacity = capacity;
            this.window = window;
        }
    }

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    /** Consumes one token, or throws TooManyRequestsException (429 with Retry-After). */
    public void consumeOrThrow(Policy policy, String key, String message) {
        ConsumptionProbe probe = bucket(policy, key).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(message, retryAfterSeconds(probe.getNanosToWaitForRefill()));
        }
    }

    /** Throws if the bucket is already empty, without consuming (check before an attempt). */
    public void requireAvailable(Policy policy, String key, String message) {
        EstimationProbe probe = bucket(policy, key).estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            throw new TooManyRequestsException(message, retryAfterSeconds(probe.getNanosToWaitForRefill()));
        }
    }

    /** Records one event (e.g. a failed login) without rejecting the current request. */
    public void record(Policy policy, String key) {
        bucket(policy, key).tryConsume(1);
    }

    public void reset(Policy policy, String key) {
        buckets.invalidate(cacheKey(policy, key));
    }

    private Bucket bucket(Policy policy, String key) {
        return buckets.get(cacheKey(policy, key), k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity)
                        .refillIntervally(policy.capacity, policy.window)
                        .build())
                .build());
    }

    private static String cacheKey(Policy policy, String key) {
        return policy.name() + ":" + (key == null ? "" : key.trim().toLowerCase(Locale.ROOT));
    }

    private static long retryAfterSeconds(long nanosToWait) {
        return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanosToWait) + 1);
    }
}
