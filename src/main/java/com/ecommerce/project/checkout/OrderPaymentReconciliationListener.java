package com.ecommerce.project.checkout;

import com.ecommerce.project.payment.event.PaymentEventOccurredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Bridges payment-provider outcomes (already normalized + deduped by the payment package) into
// order/inventory/cart state. Lives in checkout, not payment, so the payment package never has
// to know what an Order or a Cart is - see PaymentEventOccurredEvent's comment. The handling
// itself lives in PaymentReconciliationService, shared with the no-webhook reconciliation path.
@Component
@RequiredArgsConstructor
public class OrderPaymentReconciliationListener {

    private final PaymentReconciliationService paymentReconciliationService;

    @EventListener
    public void onPaymentEvent(PaymentEventOccurredEvent wrapper) {
        paymentReconciliationService.handleProviderEvent(wrapper.getPaymentEvent());
    }
}
