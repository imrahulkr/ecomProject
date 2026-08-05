package com.ecommerce.project.checkout;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // Deliberately not @Transactional: the insert attempt below is a single Spring Data JPA call,
    // which gets its own transaction from the repository proxy. Wrapping this whole method in one
    // @Transactional would let a failed insert poison that same transaction for the fallback
    // lookup that follows - the same "doomed session" pitfall as WebhookDedupService (see its
    // comment for the full explanation).
    public IdempotencyClaim begin(Long userId, String idempotencyKey, IdempotencyEndpoint endpoint, String requestHash) {
        IdempotencyRecord record = new IdempotencyRecord(userId, idempotencyKey, endpoint, requestHash);
        try {
            idempotencyRepository.saveAndFlush(record);
            return IdempotencyClaim.begin(record.getId());
        } catch (DataIntegrityViolationException e) {
            return resolveExisting(userId, idempotencyKey, endpoint, requestHash);
        }
    }

    private IdempotencyClaim resolveExisting(Long userId, String idempotencyKey, IdempotencyEndpoint endpoint, String requestHash) {
        IdempotencyRecord existing = idempotencyRepository
                .findByUserIdAndIdempotencyKeyAndEndpoint(userId, idempotencyKey, endpoint)
                .orElseThrow(() -> new IdempotencyConflictException("Could not resolve idempotency key, please retry"));

        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("This Idempotency-Key was already used with a different request");
        }

        return switch (existing.getStatus()) {
            case COMPLETED -> IdempotencyClaim.replay(existing.getResponseBody());
            case IN_PROGRESS -> throw new IdempotencyConflictException(
                    "A request with this Idempotency-Key is already being processed");
            case FAILED -> reclaim(existing.getId());
        };
    }

    private IdempotencyClaim reclaim(Long recordId) {
        int updated = idempotencyRepository.reclaimIfFailed(recordId);
        if (updated == 0) {
            throw new IdempotencyConflictException("Please retry");
        }
        return IdempotencyClaim.begin(recordId);
    }

    @Transactional
    public void complete(Long recordId, Long orderId, String responseBody) {
        idempotencyRepository.findById(recordId).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setOrderId(orderId);
            record.setResponseBody(responseBody);
            idempotencyRepository.save(record);
        });
    }

    @Transactional
    public void fail(Long recordId) {
        idempotencyRepository.findById(recordId).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            idempotencyRepository.save(record);
        });
    }
}
