package com.ecommerce.project.checkout;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.cart.CartService;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.inventory.InventoryService;
import com.ecommerce.project.notification.email.event.OnOrderPaidEvent;
import com.ecommerce.project.notification.email.event.OnPaymentFailedEvent;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.event.PaymentEvent;
import com.ecommerce.project.payment.event.PaymentEventOccurredEvent;
import com.ecommerce.project.payment.event.PaymentEventType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Bridges payment-provider outcomes (already normalized + deduped by the payment package) into
// order/inventory/cart state. Lives in checkout, not payment, so the payment package never has
// to know what an Order or a Cart is - see PaymentEventOccurredEvent's comment.
@Component
@RequiredArgsConstructor
public class OrderPaymentReconciliationListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderPaymentReconciliationListener.class);

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final ApplicationEventPublisher eventPublisher;

    // The publishing webhook controllers aren't themselves transactional (WebhookDedupService
    // already committed its own REQUIRES_NEW transaction by the time this listener runs), so this
    // listener needs its own transaction boundary to do order/reservation/attempt updates atomically.
    @EventListener
    @Transactional
    public void onPaymentEvent(PaymentEventOccurredEvent wrapper) {
        PaymentEvent event = wrapper.getPaymentEvent();
        if (event.getType() == PaymentEventType.REFUND_ISSUED) {
            return;
        }

        Optional<PaymentAttempt> attemptOpt = paymentAttemptRepository
                .findFirstByProviderNameAndProviderPaymentReferenceOrderByCreatedAtDesc(
                        event.getProviderName(), event.getProviderPaymentReference());
        if (attemptOpt.isEmpty()) {
            logger.warn("No PaymentAttempt found for provider={} reference={} - ignoring event",
                    event.getProviderName(), event.getProviderPaymentReference());
            return;
        }

        PaymentAttempt attempt = attemptOpt.get();
        Order order = attempt.getOrder();

        if (event.getType() == PaymentEventType.PAYMENT_SUCCEEDED) {
            handleSuccess(attempt, order);
        } else if (event.getType() == PaymentEventType.PAYMENT_FAILED) {
            handleFailure(attempt, order, event.getFailureReason());
        }
    }

    private void handleSuccess(PaymentAttempt attempt, Order order) {
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            // Already PAID/CANCELLED - a duplicate event with a different id, or arrived after
            // another attempt already settled this order. Nothing left to do.
            return;
        }

        attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
        paymentAttemptRepository.save(attempt);

        order.setOrderStatus(OrderStatus.PAID.name());
        orderRepository.save(order);

        inventoryService.confirmReservationsForOrder(order.getOrderId());

        Cart cart = cartRepository.findCartByEmail(order.getEmail());
        if (cart != null) {
            for (OrderItem item : order.getItems()) {
                try {
                    cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
                } catch (ResourceNotFoundException ignored) {
                    // Already removed from the cart - nothing to clean up.
                }
            }
        }

        eventPublisher.publishEvent(new OnOrderPaidEvent(this, order));
    }

    private void handleFailure(PaymentAttempt attempt, Order order, String reason) {
        if (attempt.getStatus() == PaymentAttemptStatus.SUCCEEDED
                || !OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            return;
        }

        attempt.setStatus(PaymentAttemptStatus.FAILED);
        attempt.setFailureReason(reason);
        paymentAttemptRepository.save(attempt);

        eventPublisher.publishEvent(new OnPaymentFailedEvent(this, order, reason));
    }
}
