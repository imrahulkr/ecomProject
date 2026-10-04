package com.ecommerce.project.payment.adapter.stripe;

import com.stripe.net.RequestOptions;
import com.ecommerce.project.payment.PaymentProvider;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.dto.CreatePaymentIntentRequest;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.dto.PaymentStatusResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripePaymentProvider implements PaymentProvider {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Override
    public ProviderName getProviderName() {
        return ProviderName.STRIPE;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(CreatePaymentIntentRequest request) {
        try {
            StripeClient client = new StripeClient(stripeApiKey);
            Customer customer = findOrCreateCustomer(client, request);

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(request.amountMinorUnits())
                    .setCurrency(request.currency().toLowerCase())
                    .setCustomer(customer.getId())
                    .setDescription(request.description())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build());

            if (request.orderReference() != null) {
                paramsBuilder.putMetadata("orderReference", request.orderReference());
            }
            if (request.metadata() != null) {
                for (Map.Entry<String, String> entry : request.metadata().entrySet()) {
                    paramsBuilder.putMetadata(entry.getKey(), entry.getValue());
                }
            }

            PaymentIntent intent = client.v1().paymentIntents().create(paramsBuilder.build());

            return PaymentIntentResult.builder()
                    .providerName(ProviderName.STRIPE)
                    .providerReferenceId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .clientPayload(Map.of())
                    .build();
        } catch (StripeException e) {
            throw new PaymentProviderException(ProviderName.STRIPE, "Failed to create Stripe payment intent: " + e.getMessage(), isRetryable(e), e);
        }
    }

    @Override
    public PaymentStatusResult fetchStatus(String providerPaymentReference) {
        try {
            PaymentIntent intent = new StripeClient(stripeApiKey).v1().paymentIntents().retrieve(providerPaymentReference);
            PaymentStatusResult.State state = switch (intent.getStatus()) {
                case "succeeded" -> PaymentStatusResult.State.SUCCEEDED;
                case "canceled" -> PaymentStatusResult.State.CANCELLED;
                // requires_payment_method (incl. after a decline), requires_action, processing, ...
                default -> PaymentStatusResult.State.PENDING;
            };
            return PaymentStatusResult.builder()
                    .state(state)
                    .providerPaymentId(intent.getId())
                    .amountMinorUnits(intent.getAmount())
                    .currency(intent.getCurrency())
                    .build();
        } catch (StripeException e) {
            throw new PaymentProviderException(ProviderName.STRIPE, "Failed to fetch Stripe payment intent: " + e.getMessage(), isRetryable(e), e);
        }
    }

    @Override
    public void cancel(String providerPaymentReference) {
        try {
            new StripeClient(stripeApiKey).v1().paymentIntents().cancel(providerPaymentReference);
        } catch (StripeException e) {
            // payment_intent_unexpected_state = already succeeded or already canceled.
            throw new PaymentProviderException(ProviderName.STRIPE, "Failed to cancel Stripe payment intent: " + e.getMessage(), isRetryable(e), e);
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        try {
            StripeClient client = new StripeClient(stripeApiKey);
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(request.providerPaymentId());
            if (request.amountMinorUnits() != null) {
                paramsBuilder.setAmount(request.amountMinorUnits());
            }
            RequestOptions options = request.idempotencyKey() != null
                    ? RequestOptions.builder().setIdempotencyKey(request.idempotencyKey()).build()
                    : RequestOptions.getDefault();
            Refund refund = client.v1().refunds().create(paramsBuilder.build(), options);
            return RefundResult.builder()
                    .providerName(ProviderName.STRIPE)
                    .providerRefundId(refund.getId())
                    .status(refund.getStatus())
                    .amountMinorUnits(refund.getAmount())
                    .build();
        } catch (StripeException e) {
            // A retried refund (reconciliation retries REFUND_PENDING attempts) for a payment an
            // earlier try already refunded - the outcome the caller wants, so report success.
            if ("charge_already_refunded".equals(e.getCode())) {
                return RefundResult.builder()
                        .providerName(ProviderName.STRIPE)
                        .status("succeeded")
                        .build();
            }
            throw new PaymentProviderException(ProviderName.STRIPE, "Failed to create Stripe refund: " + e.getMessage(), isRetryable(e), e);
        }
    }

    private Customer findOrCreateCustomer(StripeClient client, CreatePaymentIntentRequest request) throws StripeException {
        CustomerSearchParams searchParams = CustomerSearchParams.builder()
                .setQuery("email:'" + request.customerEmail() + "'")
                .build();
        StripeSearchResult<Customer> result = client.v1().customers().search(searchParams);
        if (!result.getData().isEmpty()) {
            return result.getData().getFirst();
        }
        CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setName(request.customerName())
                .setEmail(request.customerEmail())
                .build();
        return client.v1().customers().create(createParams);
    }

    // Card-declined/validation failures won't change on retry; network/rate-limit/5xx-class failures might.
    private boolean isRetryable(StripeException e) {
        String errorType = e.getStripeError() != null ? e.getStripeError().getType() : null;
        String code = e.getCode();
        boolean isCardError = code != null && code.startsWith("card_");
        boolean isValidationError = "invalid_request_error".equals(errorType);
        return !isCardError && !isValidationError;
    }
}
