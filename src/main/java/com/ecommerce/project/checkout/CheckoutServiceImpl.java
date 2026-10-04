package com.ecommerce.project.checkout;

import com.ecommerce.project.checkout.dto.CheckoutRequest;
import com.ecommerce.project.checkout.dto.CheckoutResponse;
import com.ecommerce.project.checkout.dto.RetryPaymentRequest;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.inventory.ReservationStatus;
import com.ecommerce.project.inventory.StockReservationRepository;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.PaymentProvider;
import com.ecommerce.project.payment.PaymentProviderRegistry;
import com.ecommerce.project.payment.ProviderHealthService;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.dto.CreatePaymentIntentRequest;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutServiceImpl.class);

    private final CheckoutTransactionExecutor checkoutTransactionExecutor;
    private final IdempotencyService idempotencyService;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final ProviderHealthService providerHealthService;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final StockReservationRepository stockReservationRepository;
    private final PaymentReconciliationService paymentReconciliationService;

    // Not a Spring bean here: this app has no autoconfigured ObjectMapper bean available (it uses
    // spring-boot-starter-webmvc rather than the full starter-web), so AuthEntryPointJwt sets the
    // same precedent of instantiating its own rather than relying on injection.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CheckoutResponse checkout(Long userId, String userEmail, String idempotencyKey, CheckoutRequest request) {
        String requestHash = IdempotencyService.hash(userId + "|" + request.addressId() + "|" + request.provider());
        IdempotencyClaim claim = idempotencyService.begin(userId, idempotencyKey, IdempotencyEndpoint.CHECKOUT, requestHash);
        if (claim.isReplay()) {
            return readReplay(claim);
        }

        OrderCreationResult created;
        try {
            created = checkoutTransactionExecutor.createOrderWithReservation(userEmail, userId, request.addressId());
        } catch (RuntimeException e) {
            idempotencyService.fail(claim.getRecordId());
            throw e;
        }

        ProviderName providerName = request.provider() != null ? request.provider() : pickHealthiestProvider();

        CheckoutResponse response = attemptPaymentSafely(created.order(), providerName);
        idempotencyService.complete(claim.getRecordId(), created.order().getOrderId(), writeJson(response));
        return response;
    }

    @Override
    public CheckoutResponse retryPayment(Long userId, String userEmail, Long orderId, String idempotencyKey, RetryPaymentRequest request) {
        String requestHash = IdempotencyService.hash(userId + "|" + orderId + "|" + request.provider());
        IdempotencyClaim claim = idempotencyService.begin(userId, idempotencyKey, IdempotencyEndpoint.RETRY_PAYMENT, requestHash);
        if (claim.isReplay()) {
            return readReplay(claim);
        }

        Order order;
        try {
            loadPendingOrder(orderId, userEmail);

            // An earlier attempt may have been paid without its webhook reaching us yet - settle
            // that before creating another payment the customer could also complete.
            paymentReconciliationService.reconcileOrder(orderId);
            order = loadPendingOrder(orderId, userEmail);

            List<?> activeReservations = stockReservationRepository
                    .findByOrder_OrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
            if (activeReservations.isEmpty()) {
                throw new APIException("The stock reservation for this order has expired - please checkout again");
            }

            // Only one payment per order may stay payable: retire the earlier attempts. If one of
            // them still completes (the cancel lost a race), reconciliation refunds it.
            paymentReconciliationService.cancelOpenAttempts(orderId);
        } catch (RuntimeException e) {
            idempotencyService.fail(claim.getRecordId());
            throw e;
        }

        CheckoutResponse response = attemptPaymentSafely(order, request.provider());
        idempotencyService.complete(claim.getRecordId(), orderId, writeJson(response));
        return response;
    }

    private Order loadPendingOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findByOrderIdAndEmail(orderId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new APIException("Order is not awaiting payment");
        }
        return order;
    }

    private ProviderName pickHealthiestProvider() {
        List<ProviderName> ranked = providerHealthService.rankProviders(paymentProviderRegistry.allProviderNames());
        if (ranked.isEmpty()) {
            throw new APIException("No payment providers are configured");
        }
        return ranked.get(0);
    }

    // The order already exists at this point, so an unexpected error must still complete the
    // idempotency record (as "payment attempt failed") - left IN_PROGRESS, a client retry with the
    // same key would later be allowed to run checkout again and create a second order.
    private CheckoutResponse attemptPaymentSafely(Order order, ProviderName providerName) {
        try {
            return attemptPayment(order, providerName);
        } catch (RuntimeException e) {
            logger.error("Unexpected error starting payment for order {} via {}", order.getOrderId(), providerName, e);
            return CheckoutResponse.builder()
                    .orderId(order.getOrderId())
                    .orderStatus(order.getOrderStatus())
                    .provider(providerName)
                    .amountMinorUnits(order.getAmountMinorUnits())
                    .currency(order.getCurrency())
                    .paymentAttemptFailed(true)
                    .failureReason("Payment could not be started, please try again")
                    .build();
        }
    }

    // Deliberately outside any DB transaction: this makes a real network call to the payment
    // provider, and a DB transaction should never sit open across external I/O.
    private CheckoutResponse attemptPayment(Order order, ProviderName providerName) {
        PaymentProvider provider = paymentProviderRegistry.get(providerName);
        long amountMinorUnits = order.getAmountMinorUnits();
        String currency = order.getCurrency();

        CreatePaymentIntentRequest intentRequest = CreatePaymentIntentRequest.builder()
                .orderReference("ORDER-" + order.getOrderId())
                .amountMinorUnits(amountMinorUnits)
                .currency(currency)
                .customerEmail(order.getEmail())
                .customerName(order.getEmail())
                .description("Payment for order " + order.getOrderId())
                .metadata(Map.of("orderId", String.valueOf(order.getOrderId())))
                .build();

        PaymentAttempt attempt = new PaymentAttempt(order, providerName, amountMinorUnits, currency);
        try {
            PaymentIntentResult result = provider.createPaymentIntent(intentRequest);
            providerHealthService.recordSuccess(providerName);
            attempt.setProviderPaymentReference(result.providerReferenceId());
            paymentAttemptRepository.save(attempt);

            return CheckoutResponse.builder()
                    .orderId(order.getOrderId())
                    .orderStatus(order.getOrderStatus())
                    .provider(providerName)
                    .amountMinorUnits(amountMinorUnits)
                    .currency(currency)
                    .clientSecret(result.clientSecret())
                    .clientPayload(result.clientPayload())
                    .paymentAttemptFailed(false)
                    .build();
        } catch (PaymentProviderException e) {
            providerHealthService.recordFailure(providerName);
            attempt.setStatus(PaymentAttemptStatus.FAILED);
            attempt.setFailureReason(e.getMessage());
            paymentAttemptRepository.save(attempt);

            logger.warn("Payment attempt failed for order {} via {}: {}", order.getOrderId(), providerName, e.getMessage());

            return CheckoutResponse.builder()
                    .orderId(order.getOrderId())
                    .orderStatus(order.getOrderStatus())
                    .provider(providerName)
                    .amountMinorUnits(amountMinorUnits)
                    .currency(currency)
                    .paymentAttemptFailed(true)
                    .failureReason(buildFailureMessage(e))
                    .build();
        }
    }

    private String buildFailureMessage(PaymentProviderException e) {
        String base = "Payment could not be started, please try again";
        return e.isRetryable() ? base : base + " with a different payment method";
    }

    private CheckoutResponse readReplay(IdempotencyClaim claim) {
        try {
            return objectMapper.readValue(claim.getReplayResponseBody(), CheckoutResponse.class);
        } catch (JsonProcessingException e) {
            throw new APIException("Failed to replay idempotent response", e);
        }
    }

    private String writeJson(CheckoutResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new APIException("Failed to serialize checkout response", e);
        }
    }
}
