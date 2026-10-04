package com.ecommerce.project.refund;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.notification.email.event.OnRefundProcessedEvent;
import com.ecommerce.project.order.FulfillmentStatus;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.order.OrderItemRepository;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payout.SellerLedgerService;
import com.ecommerce.project.product.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Set;

// The database half of refunding an item: state change, restock, ledger reversal and the refund
// row commit together under the order's row lock (same lock OrderPaymentTransitions uses), and
// the provider call happens afterwards in RefundService with no transaction open.
@Component
@RequiredArgsConstructor
class RefundTransitions {

    private static final Set<FulfillmentStatus> REFUNDED_STATES = EnumSet.of(FulfillmentStatus.CANCELLED, FulfillmentStatus.RETURNED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ProductRepository productRepository;
    private final RefundRepository refundRepository;
    private final SellerLedgerService sellerLedgerService;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    record PreparedRefund(Long refundId, ProviderName providerName, String providerPaymentId,
                          String providerPaymentReference, long amountMinorUnits, String currency, String reason) {}

    /**
     * PENDING -> CANCELLED (seller/admin cancels before shipping) or RETURN_REQUESTED -> RETURNED
     * (seller/admin approves a return). sellerIdOrNull restricts the item to that seller.
     */
    @Transactional
    PreparedRefund prepare(Long orderItemId, Long sellerIdOrNull, FulfillmentStatus target, String reason) {
        OrderItem item = (sellerIdOrNull == null
                ? orderItemRepository.findById(orderItemId)
                : orderItemRepository.findByOrderItemIdAndSellerId(orderItemId, sellerIdOrNull))
                .orElseThrow(() -> new ResourceNotFoundException("orderItem", "orderItemId", orderItemId));
        Order order = orderRepository.findByIdForUpdate(item.getOrder().getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", item.getOrder().getOrderId()));
        // Read the item again now that the order is locked - a concurrent cancel/return of the
        // same item may have committed between the first read and the lock.
        entityManager.refresh(item);

        if (!OrderStatus.PAID.name().equals(order.getOrderStatus())) {
            throw new APIException("Order is not paid - nothing to refund");
        }
        FulfillmentStatus current = item.getFulfillmentStatus();
        boolean allowed = (current == FulfillmentStatus.PENDING && target == FulfillmentStatus.CANCELLED)
                || (current == FulfillmentStatus.RETURN_REQUESTED && target == FulfillmentStatus.RETURNED);
        if (!allowed) {
            throw new APIException("Cannot transition fulfillment status from " + current + " to " + target);
        }

        PaymentAttempt paid = paymentAttemptRepository
                .findByOrder_OrderIdAndStatusIn(order.getOrderId(), EnumSet.of(PaymentAttemptStatus.SUCCEEDED))
                .stream().findFirst()
                .orElseThrow(() -> new APIException("No captured payment found for this order"));

        long amount = refundAmount(order, item);

        item.setFulfillmentStatus(target);
        item.setRefundStatus(RefundStatus.PENDING);
        item.setRefundedMinorUnits(amount);
        productRepository.incrementStock(item.getProduct().getProductId(), item.getQuantity());
        sellerLedgerService.recordRefund(item);

        Refund refund = new Refund();
        refund.setOrderId(order.getOrderId());
        refund.setOrderItemId(item.getOrderItemId());
        refund.setPaymentAttemptId(paid.getId());
        refund.setProviderName(paid.getProviderName());
        refund.setAmountMinorUnits(amount);
        refund.setCurrency(order.getCurrency());
        refund.setStatus(RefundStatus.PENDING);
        refund.setReason(reason);
        refund = refundRepository.save(refund);

        return new PreparedRefund(refund.getId(), paid.getProviderName(), paid.getProviderPaymentId(),
                paid.getProviderPaymentReference(), amount, order.getCurrency(), reason);
    }

    // What the customer actually paid for this item: its share of the order total after the
    // coupon discount (the order total minus shipping, split pro rata by item price). The last
    // item to be refunded takes whatever remains - rounding leftovers and the shipping fee - so a
    // fully refunded order is refunded to the paisa, and shipping is only refunded when nothing
    // was delivered and kept.
    private long refundAmount(Order order, OrderItem item) {
        long itemsTotal = order.getItems().stream()
                .mapToLong(oi -> oi.getOrderedProductPriceMinorUnits() * oi.getQuantity())
                .sum();
        long goodsPaid = order.getAmountMinorUnits() - order.getShippingMinorUnits();
        long gross = item.getOrderedProductPriceMinorUnits() * item.getQuantity();
        long share = itemsTotal == 0 ? 0 : BigInteger.valueOf(gross).multiply(BigInteger.valueOf(goodsPaid))
                .divide(BigInteger.valueOf(itemsTotal)).longValueExact();

        long remaining = order.getAmountMinorUnits() - refundRepository.sumCommittedForOrder(order.getOrderId());
        boolean lastItem = order.getItems().stream()
                .filter(oi -> !oi.getOrderItemId().equals(item.getOrderItemId()))
                .allMatch(oi -> REFUNDED_STATES.contains(oi.getFulfillmentStatus()));
        return Math.max(0, lastItem ? remaining : Math.min(share, remaining));
    }

    @Transactional
    void markSucceeded(Long refundId, String providerRefundId) {
        Refund refund = refundRepository.findById(refundId).orElseThrow();
        if (refund.getStatus() == RefundStatus.SUCCEEDED) {
            return;
        }
        refund.setStatus(RefundStatus.SUCCEEDED);
        refund.setAttempts(refund.getAttempts() + 1);
        refund.setProviderRefundId(providerRefundId);
        refund.setLastError(null);
        OrderItem item = orderItemRepository.findById(refund.getOrderItemId()).orElseThrow();
        item.setRefundStatus(RefundStatus.SUCCEEDED);
        eventPublisher.publishEvent(new OnRefundProcessedEvent(this, item, refund.getAmountMinorUnits(), refund.getReason()));
    }

    @Transactional
    void markFailed(Long refundId, String error) {
        Refund refund = refundRepository.findById(refundId).orElseThrow();
        if (refund.getStatus() == RefundStatus.SUCCEEDED) {
            return;
        }
        refund.setStatus(RefundStatus.FAILED);
        refund.setAttempts(refund.getAttempts() + 1);
        refund.setLastError(error);
        orderItemRepository.findById(refund.getOrderItemId())
                .ifPresent(item -> item.setRefundStatus(RefundStatus.FAILED));
    }
}
