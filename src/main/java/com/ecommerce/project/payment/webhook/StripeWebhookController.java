package com.ecommerce.project.payment.webhook;

import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.event.PaymentEvent;
import com.ecommerce.project.payment.event.PaymentEventHandler;
import com.ecommerce.project.payment.event.PaymentEventType;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/payments/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentEventHandler paymentEventHandler;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Rejected Stripe webhook with invalid signature");
            return ResponseEntity.badRequest().build();
        }

        StripeObject dataObject = resolveDataObject(event);
        if (dataObject == null) {
            return ResponseEntity.ok().build();
        }
        PaymentEventType mappedType;
        String reference;
        Long amountMinorUnits = null;
        String currency = null;
        String failureReason = null;

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                if (!(dataObject instanceof PaymentIntent paymentIntent)) return ResponseEntity.ok().build();
                mappedType = PaymentEventType.PAYMENT_SUCCEEDED;
                reference = paymentIntent.getId();
                amountMinorUnits = paymentIntent.getAmount();
                currency = paymentIntent.getCurrency();
            }
            case "payment_intent.payment_failed" -> {
                if (!(dataObject instanceof PaymentIntent paymentIntent)) return ResponseEntity.ok().build();
                mappedType = PaymentEventType.PAYMENT_FAILED;
                reference = paymentIntent.getId();
                amountMinorUnits = paymentIntent.getAmount();
                currency = paymentIntent.getCurrency();
                failureReason = paymentIntent.getLastPaymentError() != null
                        ? paymentIntent.getLastPaymentError().getMessage() : null;
            }
            case "refund.updated" -> {
                if (!(dataObject instanceof Refund refund) || !"succeeded".equals(refund.getStatus())) {
                    return ResponseEntity.ok().build();
                }
                mappedType = PaymentEventType.REFUND_ISSUED;
                reference = refund.getPaymentIntent();
                amountMinorUnits = refund.getAmount();
                currency = refund.getCurrency();
            }
            default -> {
                // Not an event type we react to (e.g. payment_intent.created) - acknowledge and ignore.
                return ResponseEntity.ok().build();
            }
        }

        PaymentEvent paymentEvent = PaymentEvent.builder()
                .providerName(ProviderName.STRIPE)
                .providerEventId(event.getId())
                .type(mappedType)
                .providerPaymentReference(reference)
                .amountMinorUnits(amountMinorUnits)
                .currency(currency)
                .occurredAt(Instant.ofEpochSecond(event.getCreated()))
                .failureReason(failureReason)
                .build();

        paymentEventHandler.handle(paymentEvent);
        return ResponseEntity.ok().build();
    }

    // getObject() only deserializes when the event's api_version exactly matches the SDK's
    // pinned version - on a mismatch (e.g. no api_version at all, or a different API version
    // than this SDK build) it doesn't cleanly return empty, it throws an NPE from inside the
    // SDK. deserializeUnsafe() is Stripe's documented fallback: it ignores the version check
    // and parses purely from the object's own "object" field, so we always fall back to it.
    private StripeObject resolveDataObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        try {
            Optional<StripeObject> stripeObject = deserializer.getObject();
            if (stripeObject.isPresent()) {
                return stripeObject.get();
            }
        } catch (RuntimeException e) {
            logger.debug("getObject() failed for Stripe event {}, falling back to deserializeUnsafe(): {}", event.getId(), e.getMessage());
        }
        try {
            return deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            logger.warn("Could not deserialize Stripe event {} of type {}: {}", event.getId(), event.getType(), e.getMessage());
            return null;
        }
    }
}
