package com.ecommerce.project.checkout;

import com.ecommerce.project.payout.SellerLedgerService;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.cart.CartService;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.inventory.InventoryService;
import com.ecommerce.project.inventory.StockReservation;
import com.ecommerce.project.notification.email.event.OnOrderPaidEvent;
import com.ecommerce.project.notification.email.event.OnPaymentFailedEvent;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.ProviderName;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// Every order/payment-attempt state change caused by a payment outcome, a cancellation or a hold
// expiring goes through here. Each method starts by locking the order row (SELECT ... FOR
// UPDATE), so a webhook, the reconciliation poller, a customer cancel and the expiry sweep acting
// on the same order run one after another and each sees what the previous one committed - a
// cancel can't overwrite PAID, and two sources reporting the same success can't both send the
// "order paid" email. The order is locked before the attempt is loaded so the attempt is read
// fresh inside the lock rather than served stale from the persistence context.
//
// Split out of PaymentReconciliationService for the same reason as CheckoutTransactionExecutor:
// self-invoked @Transactional doesn't apply, and the provider calls that follow these
// transitions (refund/cancel) must run outside any DB transaction.
@Component
@RequiredArgsConstructor
class OrderPaymentTransitions {

    // Attempts that haven't settled and could still be paid on the provider side.
    static final Set<PaymentAttemptStatus> OPEN_STATUSES = EnumSet.of(PaymentAttemptStatus.INITIATED, PaymentAttemptStatus.FAILED);

    private static final Set<PaymentAttemptStatus> SUCCESS_RECORDED = EnumSet.of(
            PaymentAttemptStatus.SUCCEEDED, PaymentAttemptStatus.REFUND_PENDING, PaymentAttemptStatus.REFUNDED);

    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final InventoryService inventoryService;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SellerLedgerService sellerLedgerService;

    enum SuccessOutcome {
        MARKED_PAID,
        // This attempt's success was already applied (duplicate webhook, or webhook + poller).
        ALREADY_RECORDED,
        // Money was taken but the order is already paid by another attempt or was cancelled -
        // the attempt is now REFUND_PENDING and the caller must refund it.
        REFUND_REQUIRED
    }

    record ProviderPaymentRef(Long attemptId, ProviderName providerName, String providerPaymentReference) {}

    record CancellationResult(boolean cancelled, List<ProviderPaymentRef> openPayments, Cart expiredCart) {
        static CancellationResult notCancelled() {
            return new CancellationResult(false, List.of(), null);
        }
    }

    @Transactional
    SuccessOutcome applySuccess(Long orderId, Long attemptId, String providerPaymentId) {
        Order order = lockOrder(orderId);
        PaymentAttempt attempt = loadAttempt(attemptId);

        if (SUCCESS_RECORDED.contains(attempt.getStatus())) {
            return SuccessOutcome.ALREADY_RECORDED;
        }
        if (providerPaymentId != null) {
            attempt.setProviderPaymentId(providerPaymentId);
        }

        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            attempt.setStatus(PaymentAttemptStatus.REFUND_PENDING);
            attempt.setFailureReason("Payment succeeded after the order was already " + order.getOrderStatus());
            return SuccessOutcome.REFUND_REQUIRED;
        }

        attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
        attempt.setFailureReason(null);
        order.setOrderStatus(OrderStatus.PAID.name());

        // The holds are still ACTIVE here: expiry only releases them by cancelling the order
        // under this same lock, and the order was PENDING_PAYMENT when we took it.
        inventoryService.confirmReservationsForOrder(orderId);
        sellerLedgerService.recordSale(order);

        Cart cart = cartRepository.findCartByEmail(order.getEmail());
        if (cart != null) {
            for (OrderItem item : order.getItems()) {
                try {
                    cartService.removeProductFromCart(cart, item.getProduct().getProductId());
                } catch (ResourceNotFoundException ignored) {
                    // Already removed from the cart - nothing to clean up.
                }
            }
        }

        eventPublisher.publishEvent(new OnOrderPaidEvent(this, order));
        return SuccessOutcome.MARKED_PAID;
    }

    // The order stays PENDING_PAYMENT on a failed payment: the customer can still retry (the
    // same Stripe PaymentIntent, or a new attempt) until the stock hold expires, at which point
    // expiry cancels it. That's why OrderStatus.PAYMENT_FAILED is never written.
    @Transactional
    void applyFailure(Long orderId, Long attemptId, String reason) {
        Order order = lockOrder(orderId);
        PaymentAttempt attempt = loadAttempt(attemptId);
        if (!OPEN_STATUSES.contains(attempt.getStatus())
                || !OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            return;
        }
        attempt.setStatus(PaymentAttemptStatus.FAILED);
        attempt.setFailureReason(reason);
        eventPublisher.publishEvent(new OnPaymentFailedEvent(this, order, reason));
    }

    // expired=true: the stock hold ran out (reservations recorded as EXPIRED, cart owner gets
    // the abandoned-cart email). expired=false: the customer cancelled (RELEASED).
    @Transactional
    CancellationResult cancelPendingOrder(Long orderId, boolean expired) {
        Order order = lockOrder(orderId);
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            return CancellationResult.notCancelled();
        }
        order.setOrderStatus(OrderStatus.CANCELLED.name());

        Cart expiredCart = null;
        if (expired) {
            List<StockReservation> ended = inventoryService.expireReservationsForOrder(orderId);
            expiredCart = ended.isEmpty() ? null : ended.get(0).getCart();
        } else {
            inventoryService.releaseReservationsForOrder(orderId);
        }

        return new CancellationResult(true, cancelOpenAttemptsOf(orderId), expiredCart);
    }

    // Retry path: the order stays PENDING_PAYMENT, but every earlier attempt is retired so the
    // customer can't end up paying both the old and the new one.
    @Transactional
    List<ProviderPaymentRef> cancelOpenAttempts(Long orderId) {
        Order order = lockOrder(orderId);
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            return List.of();
        }
        return cancelOpenAttemptsOf(orderId);
    }

    @Transactional
    void markRefunded(Long attemptId, String providerRefundId) {
        PaymentAttempt attempt = loadAttempt(attemptId);
        if (attempt.getStatus() != PaymentAttemptStatus.REFUND_PENDING) {
            return;
        }
        attempt.setStatus(PaymentAttemptStatus.REFUNDED);
        attempt.setFailureReason(appendNote(attempt.getFailureReason(),
                providerRefundId != null ? "refunded (" + providerRefundId + ")" : "refunded"));
    }

    @Transactional
    void recordRefundFailure(Long attemptId, String error) {
        PaymentAttempt attempt = loadAttempt(attemptId);
        if (attempt.getStatus() == PaymentAttemptStatus.REFUND_PENDING) {
            attempt.setFailureReason(appendNote(attempt.getFailureReason(), "refund failed, will retry: " + error));
        }
    }

    // Attempts with no provider reference never reached the provider (createPaymentIntent itself
    // failed) - nothing is payable, so they keep their FAILED status and history.
    private List<ProviderPaymentRef> cancelOpenAttemptsOf(Long orderId) {
        List<PaymentAttempt> payable = paymentAttemptRepository.findByOrder_OrderIdAndStatusIn(orderId, OPEN_STATUSES).stream()
                .filter(attempt -> attempt.getProviderPaymentReference() != null)
                .toList();
        payable.forEach(attempt -> attempt.setStatus(PaymentAttemptStatus.CANCELLED));
        return payable.stream()
                .map(attempt -> new ProviderPaymentRef(attempt.getId(), attempt.getProviderName(), attempt.getProviderPaymentReference()))
                .toList();
    }

    private Order lockOrder(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
    }

    private PaymentAttempt loadAttempt(Long attemptId) {
        return paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalStateException("PaymentAttempt " + attemptId + " not found"));
    }

    // failure_reason is TEXT, but a refund retried every sweep for a long outage would keep
    // appending - keep only the most recent part.
    private static String appendNote(String existing, String note) {
        String combined = existing == null ? note : existing + "; " + note;
        return combined.length() > 2000 ? combined.substring(combined.length() - 2000) : combined;
    }
}
