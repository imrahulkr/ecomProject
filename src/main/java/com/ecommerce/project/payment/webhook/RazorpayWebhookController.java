package com.ecommerce.project.payment.webhook;

import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.event.PaymentEvent;
import com.ecommerce.project.payment.event.PaymentEventHandler;
import com.ecommerce.project.payment.event.PaymentEventType;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/payments/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final PaymentEventHandler paymentEventHandler;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("X-Razorpay-Signature") String signatureHeader,
                                               @RequestHeader("X-Razorpay-Event-Id") String eventId) {
        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
        } catch (RazorpayException e) {
            logger.warn("Failed to verify Razorpay webhook signature", e);
            return ResponseEntity.badRequest().build();
        }
        if (!valid) {
            logger.warn("Rejected Razorpay webhook with invalid signature");
            return ResponseEntity.badRequest().build();
        }

        JSONObject body = new JSONObject(payload);
        String eventName = body.optString("event", "");
        JSONObject payloadObject = body.optJSONObject("payload");

        PaymentEventType mappedType;
        String reference;
        Long amountMinorUnits = null;
        String currency = null;
        String failureReason = null;

        switch (eventName) {
            case "payment.captured" -> {
                JSONObject payment = extractEntity(payloadObject, "payment");
                if (payment == null) return ResponseEntity.ok().build();
                mappedType = PaymentEventType.PAYMENT_SUCCEEDED;
                reference = payment.optString("order_id", null);
                amountMinorUnits = payment.has("amount") ? payment.getLong("amount") : null;
                currency = payment.optString("currency", null);
            }
            case "payment.failed" -> {
                JSONObject payment = extractEntity(payloadObject, "payment");
                if (payment == null) return ResponseEntity.ok().build();
                mappedType = PaymentEventType.PAYMENT_FAILED;
                reference = payment.optString("order_id", null);
                amountMinorUnits = payment.has("amount") ? payment.getLong("amount") : null;
                currency = payment.optString("currency", null);
                failureReason = payment.optString("error_description", null);
            }
            case "refund.processed" -> {
                JSONObject refund = extractEntity(payloadObject, "refund");
                if (refund == null) return ResponseEntity.ok().build();
                mappedType = PaymentEventType.REFUND_ISSUED;
                reference = refund.optString("payment_id", null);
                amountMinorUnits = refund.has("amount") ? refund.getLong("amount") : null;
                currency = refund.optString("currency", null);
            }
            default -> {
                return ResponseEntity.ok().build();
            }
        }

        if (reference == null) {
            logger.warn("Razorpay event {} (eventId={}) missing a payment/order reference; ignoring", eventName, eventId);
            return ResponseEntity.ok().build();
        }

        PaymentEvent paymentEvent = PaymentEvent.builder()
                .providerName(ProviderName.RAZORPAY)
                .providerEventId(eventId)
                .type(mappedType)
                .providerPaymentReference(reference)
                .amountMinorUnits(amountMinorUnits)
                .currency(currency)
                .occurredAt(Instant.now())
                .failureReason(failureReason)
                .build();

        paymentEventHandler.handle(paymentEvent);
        return ResponseEntity.ok().build();
    }

    private JSONObject extractEntity(JSONObject payloadObject, String key) {
        if (payloadObject == null || !payloadObject.has(key)) return null;
        JSONObject wrapper = payloadObject.optJSONObject(key);
        return wrapper == null ? null : wrapper.optJSONObject("entity");
    }
}
