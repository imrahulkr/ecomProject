package com.ecommerce.project.payment.adapter.razorpay;

import com.ecommerce.project.payment.PaymentProvider;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.dto.CreatePaymentIntentRequest;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
            orderRequest.put("amount", request.getAmountMinorUnits());
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getOrderReference());
            orderRequest.put("payment_capture", 1);
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                orderRequest.put("notes", new JSONObject(request.getMetadata()));
            }

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");
            String status = order.get("status");

            // Razorpay has no "client secret" - the frontend opens Checkout with these fields directly.
            Map<String, Object> clientPayload = new LinkedHashMap<>();
            clientPayload.put("keyId", keyId);
            clientPayload.put("razorpayOrderId", orderId);
            clientPayload.put("amount", request.getAmountMinorUnits());
            clientPayload.put("currency", request.getCurrency());

            return PaymentIntentResult.builder()
                    .providerName(ProviderName.RAZORPAY)
                    .providerReferenceId(orderId)
                    .clientSecret(null)
                    .status(status)
                    .clientPayload(clientPayload)
                    .build();
        } catch (RazorpayException e) {
            throw new PaymentProviderException(ProviderName.RAZORPAY, "Failed to create Razorpay order", true, e);
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            Refund refund;
            if (request.getAmountMinorUnits() != null) {
                JSONObject options = new JSONObject();
                options.put("amount", request.getAmountMinorUnits());
                refund = client.payments.refund(request.getProviderPaymentId(), options);
            } else {
                refund = client.payments.refund(request.getProviderPaymentId());
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
            throw new PaymentProviderException(ProviderName.RAZORPAY, "Failed to process Razorpay refund", true, e);
        }
    }
}
