package com.ecommerce.project.checkout;

import com.ecommerce.project.checkout.OrderPaymentTransitions.CancellationResult;
import com.ecommerce.project.checkout.OrderPaymentTransitions.ProviderPaymentRef;
import com.ecommerce.project.checkout.OrderPaymentTransitions.SuccessOutcome;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.notification.email.event.OnCartReservationExpiredEvent;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.PaymentProviderRegistry;
import com.ecommerce.project.payment.dto.PaymentStatusResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;
import com.ecommerce.project.payment.event.PaymentEvent;
import com.ecommerce.project.payment.event.PaymentEventType;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Owns what a payment outcome means for an order, whichever way that outcome is learned: a
// provider webhook (handleProviderEvent), or asking the provider directly when no webhook came
// (reconcile*). Also owns the ways an unpaid order ends - customer cancel and hold expiry - since
// both have to stop the provider-side payment too. Deliberately not @Transactional: every state
// change goes through OrderPaymentTransitions (one short locked transaction each), and the
// provider calls in between (fetch status, cancel, refund) run with no transaction open.
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReconciliationService.class);

    // Attempts worth asking the provider about: anything that might have been paid without us
    // hearing (incl. CANCELLED - a cancel can race the customer paying), plus refunds to retry.
    private static final Set<PaymentAttemptStatus> RECONCILABLE = EnumSet.of(
            PaymentAttemptStatus.INITIATED, PaymentAttemptStatus.FAILED,
            PaymentAttemptStatus.CANCELLED, PaymentAttemptStatus.REFUND_PENDING);

    // The order page polls the sync endpoint every few seconds while waiting for payment; this
    // keeps that from turning into one provider API call per poll per open tab.
    private static final Duration USER_SYNC_THROTTLE = Duration.ofSeconds(5);

    private final OrderPaymentTransitions transitions;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<Long, Instant> lastUserSyncByOrderId = new ConcurrentHashMap<>();

    @Value("${payment.reconciliation.min-age-seconds}")
    private long minAgeSeconds;

    @Value("${payment.reconciliation.lookback-minutes}")
    private long lookbackMinutes;

    // ---- Webhooks ----

    public void handleProviderEvent(PaymentEvent event) {
        if (event.getType() == PaymentEventType.REFUND_ISSUED) {
            return;
        }

        Optional<PaymentAttempt> attemptOpt = paymentAttemptRepository
                .findFirstByProviderNameAndProviderPaymentReferenceOrderByCreatedAtDesc(
                        event.getProviderName(), event.getProviderPaymentReference());
        if (attemptOpt.isEmpty()) {
            // Can happen if the webhook beats CheckoutServiceImpl saving the attempt; the
            // reconciliation poller picks the payment up once the attempt row exists.
            logger.warn("No PaymentAttempt found for provider={} reference={} - ignoring event",
                    event.getProviderName(), event.getProviderPaymentReference());
            return;
        }
        PaymentAttempt attempt = attemptOpt.get();

        if (!matchesAttemptAmount(event.getType(), event.getAmountMinorUnits(), event.getCurrency(), attempt)) {
            logger.warn("Ignoring payment event with mismatched amount/currency: provider={}, reference={}, expected={} {}, received={} {}",
                    event.getProviderName(), event.getProviderPaymentReference(),
                    attempt.getAmountMinorUnits(), attempt.getCurrency(),
                    event.getAmountMinorUnits(), event.getCurrency());
            return;
        }

        if (event.getType() == PaymentEventType.PAYMENT_SUCCEEDED) {
            String paymentId = event.getProviderPaymentId() != null
                    ? event.getProviderPaymentId() : event.getProviderPaymentReference();
            applySuccess(attempt, paymentId);
        } else if (event.getType() == PaymentEventType.PAYMENT_FAILED) {
            transitions.applyFailure(attempt.getOrder().getOrderId(), attempt.getId(), event.getFailureReason());
        }
    }

    // ---- Reconciliation (no webhook) ----

    // Scheduled safety net: asks the provider about every recent attempt that hasn't settled,
    // and retries refunds that failed earlier.
    public void reconcileOpenAttempts() {
        Instant now = Instant.now();
        List<PaymentAttempt> recent = paymentAttemptRepository
                .findByStatusInAndProviderPaymentReferenceIsNotNullAndCreatedAtBetween(
                        RECONCILABLE,
                        now.minus(Duration.ofMinutes(lookbackMinutes)),
                        now.minus(Duration.ofSeconds(minAgeSeconds)));
        recent.forEach(this::reconcileAttemptSafely);

        // Refunds owed are retried regardless of age - the customer's money is still held.
        paymentAttemptRepository.findByStatus(PaymentAttemptStatus.REFUND_PENDING).stream()
                .filter(attempt -> attempt.getCreatedAt().isBefore(now.minus(Duration.ofMinutes(lookbackMinutes))))
                .forEach(this::reconcileAttemptSafely);

        lastUserSyncByOrderId.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minus(USER_SYNC_THROTTLE)));
    }

    public void reconcileOrder(Long orderId) {
        paymentAttemptRepository.findByOrder_OrderIdAndStatusIn(orderId, RECONCILABLE)
                .forEach(this::reconcileAttemptSafely);
    }

    // Called by the order page while it waits for a payment to land, so a missed or slow
    // webhook doesn't leave the customer looking at "awaiting payment" after paying.
    public void syncOrderForUser(String email, Long orderId) {
        Order order = orderRepository.findByOrderIdAndEmail(orderId, email)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            return;
        }
        Instant now = Instant.now();
        Instant previous = lastUserSyncByOrderId.put(orderId, now);
        if (previous != null && previous.isAfter(now.minus(USER_SYNC_THROTTLE))) {
            return;
        }
        reconcileOrder(orderId);
    }

    // ---- Ending unpaid orders ----

    public void cancelOrderForUser(String email, Long orderId) {
        Order order = orderRepository.findByOrderIdAndEmail(orderId, email)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new APIException("Only orders awaiting payment can be cancelled");
        }

        // The customer may have paid moments ago with the webhook still in flight - settle that
        // first so we don't cancel (and then have to refund) an order that's actually paid.
        reconcileOrder(orderId);

        CancellationResult result = transitions.cancelPendingOrder(orderId, false);
        if (!result.cancelled()) {
            throw new APIException("This order has already been paid and can no longer be cancelled");
        }
        cancelProviderPayments(result.openPayments());
        logger.info("Order {} cancelled by customer", orderId);
    }

    // Retry path: retire every earlier attempt before a new one is created.
    public void cancelOpenAttempts(Long orderId) {
        cancelProviderPayments(transitions.cancelOpenAttempts(orderId));
    }

    // Scheduled: cancels orders whose stock hold has lapsed without payment.
    public void expireUnpaidOrders() {
        for (Long orderId : orderRepository.findPendingPaymentOrderIdsWithoutLiveHold(Instant.now())) {
            try {
                expireOrder(orderId);
            } catch (RuntimeException e) {
                logger.error("Failed to expire unpaid order {}", orderId, e);
            }
        }
    }

    private void expireOrder(Long orderId) {
        // A payment that succeeded without a webhook reaching us must win over expiry.
        reconcileOrder(orderId);

        CancellationResult result = transitions.cancelPendingOrder(orderId, true);
        if (!result.cancelled()) {
            return;
        }
        cancelProviderPayments(result.openPayments());
        if (result.expiredCart() != null) {
            eventPublisher.publishEvent(new OnCartReservationExpiredEvent(this, result.expiredCart()));
        }
        logger.info("Order {} cancelled: stock hold expired before payment completed", orderId);
    }

    // ---- Shared ----

    private void reconcileAttemptSafely(PaymentAttempt attempt) {
        try {
            reconcileAttempt(attempt);
        } catch (RuntimeException e) {
            logger.error("Failed to reconcile payment attempt {} (order {})", attempt.getId(), attempt.getOrder().getOrderId(), e);
        }
    }

    private void reconcileAttempt(PaymentAttempt attempt) {
        if (attempt.getProviderPaymentReference() == null) {
            return;
        }
        PaymentStatusResult status;
        try {
            status = paymentProviderRegistry.get(attempt.getProviderName())
                    .fetchStatus(attempt.getProviderPaymentReference());
        } catch (PaymentProviderException e) {
            logger.warn("Could not fetch status of payment attempt {} from {}: {}",
                    attempt.getId(), attempt.getProviderName(), e.getMessage());
            return;
        }
        if (status.state() != PaymentStatusResult.State.SUCCEEDED) {
            return;
        }

        if (attempt.getStatus() == PaymentAttemptStatus.REFUND_PENDING) {
            refund(attempt, status.providerPaymentId());
            return;
        }
        if (!matchesAttemptAmount(PaymentEventType.PAYMENT_SUCCEEDED, status.amountMinorUnits(), status.currency(), attempt)) {
            logger.warn("Provider reports payment attempt {} succeeded with mismatched amount/currency: expected={} {}, actual={} {}",
                    attempt.getId(), attempt.getAmountMinorUnits(), attempt.getCurrency(),
                    status.amountMinorUnits(), status.currency());
            return;
        }
        logger.info("Reconciled payment attempt {} (order {}) as succeeded without a webhook",
                attempt.getId(), attempt.getOrder().getOrderId());
        applySuccess(attempt, status.providerPaymentId());
    }

    private void applySuccess(PaymentAttempt attempt, String providerPaymentId) {
        SuccessOutcome outcome = transitions.applySuccess(attempt.getOrder().getOrderId(), attempt.getId(), providerPaymentId);
        if (outcome == SuccessOutcome.REFUND_REQUIRED) {
            logger.warn("Payment attempt {} succeeded but order {} can no longer use it - refunding",
                    attempt.getId(), attempt.getOrder().getOrderId());
            refund(attempt, providerPaymentId);
        }
    }

    private void refund(PaymentAttempt attempt, String providerPaymentId) {
        try {
            RefundResult result = paymentProviderRegistry.get(attempt.getProviderName()).refund(
                    RefundRequest.builder()
                            .providerPaymentId(providerPaymentId)
                            .reason("Order " + attempt.getOrder().getOrderId() + " was already paid or cancelled")
                            .idempotencyKey("orphan-refund-" + attempt.getId())
                            .build());
            transitions.markRefunded(attempt.getId(), result.providerRefundId());
            logger.info("Refunded payment attempt {} (order {})", attempt.getId(), attempt.getOrder().getOrderId());
        } catch (PaymentProviderException e) {
            // Stays REFUND_PENDING - reconcileOpenAttempts retries it on the next sweep.
            transitions.recordRefundFailure(attempt.getId(), e.getMessage());
            logger.error("Refund failed for payment attempt {} (order {}), will retry: {}",
                    attempt.getId(), attempt.getOrder().getOrderId(), e.getMessage());
        }
    }

    private void cancelProviderPayments(List<ProviderPaymentRef> payments) {
        for (ProviderPaymentRef payment : payments) {
            try {
                paymentProviderRegistry.get(payment.providerName()).cancel(payment.providerPaymentReference());
            } catch (PaymentProviderException e) {
                // Usually "already succeeded" - that success gets refunded when it's observed.
                logger.info("Could not cancel {} payment {} (attempt {}): {}",
                        payment.providerName(), payment.providerPaymentReference(), payment.attemptId(), e.getMessage());
            }
        }
    }

    private static boolean matchesAttemptAmount(PaymentEventType type, Long amountMinorUnits, String currency, PaymentAttempt attempt) {
        if (type == PaymentEventType.PAYMENT_SUCCEEDED && (amountMinorUnits == null || currency == null)) {
            return false;
        }
        if (amountMinorUnits != null && amountMinorUnits != attempt.getAmountMinorUnits()) {
            return false;
        }
        return currency == null || currency.equalsIgnoreCase(attempt.getCurrency());
    }
}
