package com.ecommerce.project.payment;

import com.ecommerce.project.payment.dto.CreatePaymentIntentRequest;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.dto.PaymentStatusResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;

public interface PaymentProvider {
    ProviderName getProviderName();

    PaymentIntentResult createPaymentIntent(CreatePaymentIntentRequest request);

    RefundResult refund(RefundRequest request);

    /**
     * Asks the provider for the current state of a payment, by the reference createPaymentIntent
     * returned. Used to reconcile when a webhook was missed.
     */
    PaymentStatusResult fetchStatus(String providerPaymentReference);

    /**
     * Stops a payment from being completed (order cancelled, hold expired, or superseded by a
     * retry). Best effort: throws PaymentProviderException if the provider refuses (e.g. it
     * already succeeded), in which case reconciliation refunds it once the success is observed.
     */
    void cancel(String providerPaymentReference);
}
