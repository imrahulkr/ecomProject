package com.ecommerce.project.payment;

import com.ecommerce.project.payment.dto.CreatePaymentIntentRequest;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;

public interface PaymentProvider {
    ProviderName getProviderName();

    PaymentIntentResult createPaymentIntent(CreatePaymentIntentRequest request);

    RefundResult refund(RefundRequest request);
}
