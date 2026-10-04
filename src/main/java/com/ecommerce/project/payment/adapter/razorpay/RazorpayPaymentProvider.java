package com.ecommerce.project.payment.adapter.razorpay;

import com.ecommerce.project.payment.PaymentProvider;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.dto.CreatePaymentIntentRequest;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.dto.PaymentStatusResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RazorpayPaymentProvider implements PaymentProvider {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Override
    public ProviderName getProviderName() {
        return ProviderName.RAZORPAY;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(CreatePaymentIntentRequest request) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.amountMinorUnits());
            orderRequest.put("currency", request.currency());
            orderRequest.put("receipt", request.orderReference());
            orderRequest.put("payment_capture", 1);
            if (request.metadata() != null && !request.metadata().isEmpty()) {
                orderRequest.put("notes", new JSONObject(request.metadata()));
            }

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");
            String status = order.get("status");

            // Razorpay has no "client secret" - the frontend opens Checkout with these fields directly.
            Map<String, Object> clientPayload = new LinkedHashMap<>();
            clientPayload.put("keyId", keyId);
            clientPayload.put("razorpayOrderId", orderId);
            clientPayload.put("amount", request.amountMinorUnits());
            clientPayload.put("currency", request.currency());

            return PaymentIntentResult.builder()
                    .providerName(ProviderName.RAZORPAY)
                    .providerReferenceId(orderId)
                    .clientSecret(null)
                    .status(status)
                    .clientPayload(clientPayload)
                    .build();
        } catch (RazorpayException e) {
            throw new PaymentProviderException(ProviderName.RAZORPAY, "Failed to create Razorpay order: " + e.getMessage(), true, e);
        }
    }

    // The payment reference is a Razorpay order id; the order itself has no "succeeded" state
    // worth trusting on its own, so look for a captured payment against it (payment_capture=1
    // auto-captures, so "authorized" is only a brief in-between state and stays PENDING).
    @Override
    public PaymentStatusResult fetchStatus(String providerPaymentReference) {
        try {
            List<Payment> payments = new RazorpayClient(keyId, keySecret).orders.fetchPayments(providerPaymentReference);
            for (Payment payment : payments) {
                if ("captured".equals(payment.get("status"))) {
                    Object amount = payment.get("amount");
                    return PaymentStatusResult.builder()
                            .state(PaymentStatusResult.State.SUCCEEDED)
                            .providerPaymentId(payment.get("id"))
                            .amountMinorUnits(amount instanceof Number n ? n.longValue() : null)
                            .currency(payment.get("currency"))
                            .build();
                }
            }
            return PaymentStatusResult.builder().state(PaymentStatusResult.State.PENDING).build();
        } catch (RazorpayException e) {
            throw new PaymentProviderException(ProviderName.RAZORPAY, "Failed to fetch Razorpay order payments: " + e.getMessage(), true, e);
        }
    }

    // Razorpay has no API to cancel an order - it simply stays payable until it expires. A
    // payment that still lands on a cancelled order is caught by reconciliation and refunded.
    @Override
    public void cancel(String providerPaymentReference) {
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            Refund refund;
            if (request.amountMinorUnits() != null) {
                JSONObject options = new JSONObject();
                options.put("amount", request.amountMinorUnits());
                refund = client.payments.refund(request.providerPaymentId(), options);
            } else {
                refund = client.payments.refund(request.providerPaymentId());
            }
            String refundId = refund.get("id");
            String status = refund.get("status");
            Integer amount = refund.get("amount");

            return RefundResult.builder()
                    .providerName(ProviderName.RAZORPAY)
                    .providerRefundId(refundId)
                    .status(status)
                    .amountMinorUnits(amount == null ? null : amount.longValue())
                    .build();
        } catch (RazorpayException e) {
            // A retried refund for a payment an earlier try already refunded - the outcome the
            // caller wants (Razorpay has no error code for this, only the message).
            if (e.getMessage() != null && e.getMessage().contains("fully refunded")) {
                return RefundResult.builder()
                        .providerName(ProviderName.RAZORPAY)
                        .status("processed")
                        .build();
            }
            throw new PaymentProviderException(ProviderName.RAZORPAY, "Failed to process Razorpay refund: " + e.getMessage(), true, e);
        }
    }
}
