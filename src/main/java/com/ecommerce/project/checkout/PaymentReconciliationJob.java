package com.ecommerce.project.checkout;

import com.ecommerce.project.refund.RefundService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentReconciliationService paymentReconciliationService;
    private final RefundService refundService;

    // Reconcile before expiring, so an order whose payment succeeded without a webhook is marked
    // paid rather than cancelled (expireUnpaidOrders also re-checks each order it's about to end).
    @Scheduled(
            initialDelayString = "${payment.reconciliation.interval-ms}",
            fixedDelayString = "${payment.reconciliation.interval-ms}"
    )
    public void run() {
        try {
            paymentReconciliationService.reconcileOpenAttempts();
        } catch (Exception e) {
            // Never let one bad sweep kill the scheduler thread - the next tick retries.
            logger.error("Payment reconciliation sweep failed", e);
        }
        try {
            paymentReconciliationService.expireUnpaidOrders();
        } catch (Exception e) {
            logger.error("Unpaid order expiry sweep failed", e);
        }
        try {
            refundService.retryUnfinishedRefunds();
        } catch (Exception e) {
            logger.error("Item refund retry sweep failed", e);
        }
    }
}
