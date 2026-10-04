package com.ecommerce.project.refund;

import com.ecommerce.project.order.FulfillmentStatus;
import com.ecommerce.project.order.OrderItemRepository;
import com.ecommerce.project.order.dto.OrderItemDTO;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentProvider;
import com.ecommerce.project.payment.PaymentProviderRegistry;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import com.ecommerce.project.refund.RefundTransitions.PreparedRefund;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Refunds a single order item - a seller/admin cancelling it before shipping, or approving a
// customer's return. Stock goes back on sale and the seller's earnings are reversed immediately;
// the money is refunded through the provider right after, and retried by the reconciliation job
// if the provider call fails. Deliberately not @Transactional - see RefundTransitions.
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);
    static final int MAX_ATTEMPTS = 10;
    // A PENDING refund this old was interrupted between the database commit and the provider call.
    private static final Duration STALE_PENDING = Duration.ofMinutes(5);

    private final RefundTransitions transitions;
    private final RefundRepository refundRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;

    public static boolean isRefunding(FulfillmentStatus target) {
        return target == FulfillmentStatus.CANCELLED || target == FulfillmentStatus.RETURNED;
    }

    public static String reasonFor(FulfillmentStatus target, String actor) {
        return target == FulfillmentStatus.CANCELLED ? "Cancelled by the " + actor : "Return approved by the " + actor;
    }

    public OrderItemDTO refundItem(Long orderItemId, Long sellerIdOrNull, FulfillmentStatus target, String reason) {
        PreparedRefund prepared = transitions.prepare(orderItemId, sellerIdOrNull, target, reason);
        execute(prepared);
        return orderItemRepository.findById(orderItemId)
                .map(item -> modelMapper.map(item, OrderItemDTO.class))
                .orElseThrow();
    }

    // Scheduled (PaymentReconciliationJob): retries failed refunds and finishes interrupted ones.
    public void retryUnfinishedRefunds() {
        Instant stalePendingBefore = Instant.now().minus(STALE_PENDING);
        List<Refund> unfinished = refundRepository.findByStatusIn(List.of(RefundStatus.PENDING, RefundStatus.FAILED));
        for (Refund refund : unfinished) {
            if (refund.getAttempts() >= MAX_ATTEMPTS) {
                continue; // left FAILED for manual follow-up (see the refunds table)
            }
            if (refund.getStatus() == RefundStatus.PENDING && refund.getCreatedAt().isAfter(stalePendingBefore)) {
                continue; // probably still in flight in refundItem
            }
            PaymentAttempt paid = paymentAttemptRepository.findById(refund.getPaymentAttemptId()).orElse(null);
            if (paid == null) {
                continue;
            }
            execute(new PreparedRefund(refund.getId(), refund.getProviderName(), paid.getProviderPaymentId(),
                    paid.getProviderPaymentReference(), refund.getAmountMinorUnits(), refund.getCurrency(), refund.getReason()));
        }
    }

    private void execute(PreparedRefund refund) {
        if (refund.amountMinorUnits() == 0) {
            // e.g. a fully discounted item - nothing to send back, but still record the outcome.
            transitions.markSucceeded(refund.refundId(), null);
            return;
        }
        try {
            PaymentProvider provider = paymentProviderRegistry.get(refund.providerName());
            String paymentId = refund.providerPaymentId() != null
                    ? refund.providerPaymentId()
                    // Attempts paid before provider_payment_id was recorded: ask the provider.
                    : provider.fetchStatus(refund.providerPaymentReference()).providerPaymentId();
            RefundResult result = provider.refund(RefundRequest.builder()
                    .providerPaymentId(paymentId)
                    .amountMinorUnits(refund.amountMinorUnits())
                    .reason(refund.reason())
                    // Same key on every retry, so a retry after an unrecorded success can't refund twice.
                    .idempotencyKey("item-refund-" + refund.refundId())
                    .build());
            transitions.markSucceeded(refund.refundId(), result.providerRefundId());
            logger.info("Refunded {} {} for refund {}", refund.amountMinorUnits(), refund.currency(), refund.refundId());
        } catch (PaymentProviderException e) {
            transitions.markFailed(refund.refundId(), e.getMessage());
            logger.error("Refund {} failed, will retry: {}", refund.refundId(), e.getMessage());
        }
    }
}
