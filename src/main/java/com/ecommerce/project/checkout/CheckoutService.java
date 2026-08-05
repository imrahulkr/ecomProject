package com.ecommerce.project.checkout;

import com.ecommerce.project.checkout.dto.CheckoutRequest;
import com.ecommerce.project.checkout.dto.CheckoutResponse;
import com.ecommerce.project.checkout.dto.RetryPaymentRequest;

public interface CheckoutService {

    CheckoutResponse checkout(Long userId, String userEmail, String idempotencyKey, CheckoutRequest request);

    CheckoutResponse retryPayment(Long userId, String userEmail, Long orderId, String idempotencyKey, RetryPaymentRequest request);
}
