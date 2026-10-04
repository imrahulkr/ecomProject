package com.ecommerce.project.checkout;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.checkout.OrderPaymentTransitions.CancellationResult;
import com.ecommerce.project.checkout.OrderPaymentTransitions.ProviderPaymentRef;
import com.ecommerce.project.checkout.OrderPaymentTransitions.SuccessOutcome;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.notification.email.event.OnCartReservationExpiredEvent;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.PaymentProvider;
import com.ecommerce.project.payment.PaymentProviderRegistry;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.dto.PaymentStatusResult;
import com.ecommerce.project.payment.dto.RefundRequest;
import com.ecommerce.project.payment.dto.RefundResult;
import com.ecommerce.project.payment.event.PaymentEvent;
import com.ecommerce.project.payment.event.PaymentEventType;
import com.ecommerce.project.payment.exception.PaymentProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentReconciliationServiceTest {

    @Mock
    private OrderPaymentTransitions transitions;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentProviderRegistry paymentProviderRegistry;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PaymentProvider stripe;

    @InjectMocks
    private PaymentReconciliationService service;

    private Order order;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(9L);
        order.setEmail("buyer@example.com");
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());

        attempt = new PaymentAttempt(order, ProviderName.STRIPE, 1_680_000L, "INR");
        attempt.setId(6L);
        attempt.setProviderPaymentReference("pi_123");

        when(paymentProviderRegistry.get(ProviderName.STRIPE)).thenReturn(stripe);
        when(paymentAttemptRepository.findByOrder_OrderIdAndStatusIn(eq(9L), any())).thenReturn(List.of(attempt));
        when(orderRepository.findByOrderIdAndEmail(9L, "buyer@example.com")).thenReturn(Optional.of(order));
    }

    private static PaymentStatusResult succeeded(long amount, String currency) {
        return PaymentStatusResult.builder()
                .state(PaymentStatusResult.State.SUCCEEDED)
                .providerPaymentId("pi_123")
                .amountMinorUnits(amount)
                .currency(currency)
                .build();
    }

    @Test
    void reconcileOrder_providerReportsSuccess_appliesIt() {
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(1_680_000L, "inr"));
        when(transitions.applySuccess(9L, 6L, "pi_123")).thenReturn(SuccessOutcome.MARKED_PAID);

        service.reconcileOrder(9L);

        verify(transitions).applySuccess(9L, 6L, "pi_123");
        verify(stripe, never()).refund(any());
    }

    @Test
    void reconcileOrder_stillPending_changesNothing() {
        when(stripe.fetchStatus("pi_123")).thenReturn(
                PaymentStatusResult.builder().state(PaymentStatusResult.State.PENDING).build());

        service.reconcileOrder(9L);

        verify(transitions, never()).applySuccess(anyLong(), anyLong(), any());
    }

    @Test
    void reconcileOrder_amountMismatch_isIgnored() {
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(100L, "inr"));

        service.reconcileOrder(9L);

        verify(transitions, never()).applySuccess(anyLong(), anyLong(), any());
    }

    @Test
    void reconcileOrder_providerUnreachable_isSwallowed() {
        when(stripe.fetchStatus("pi_123")).thenThrow(
                new PaymentProviderException(ProviderName.STRIPE, "timeout", true, null));

        service.reconcileOrder(9L);

        verify(transitions, never()).applySuccess(anyLong(), anyLong(), any());
    }

    @Test
    void successForOrderThatCannotUseIt_isRefunded() {
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(1_680_000L, "inr"));
        when(transitions.applySuccess(9L, 6L, "pi_123")).thenReturn(SuccessOutcome.REFUND_REQUIRED);
        when(stripe.refund(any())).thenReturn(RefundResult.builder().providerRefundId("re_1").build());

        service.reconcileOrder(9L);

        ArgumentCaptor<RefundRequest> refund = ArgumentCaptor.forClass(RefundRequest.class);
        verify(stripe).refund(refund.capture());
        assertThat(refund.getValue().providerPaymentId()).isEqualTo("pi_123");
        assertThat(refund.getValue().amountMinorUnits()).isNull();
        verify(transitions).markRefunded(6L, "re_1");
    }

    @Test
    void failedRefund_isRecordedForRetry() {
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(1_680_000L, "inr"));
        when(transitions.applySuccess(9L, 6L, "pi_123")).thenReturn(SuccessOutcome.REFUND_REQUIRED);
        when(stripe.refund(any())).thenThrow(new PaymentProviderException(ProviderName.STRIPE, "down", true, null));

        service.reconcileOrder(9L);

        verify(transitions).recordRefundFailure(6L, "down");
        verify(transitions, never()).markRefunded(anyLong(), any());
    }

    @Test
    void refundPendingAttempt_isRefundedWithoutReapplyingSuccess() {
        attempt.setStatus(PaymentAttemptStatus.REFUND_PENDING);
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(1_680_000L, "inr"));
        when(stripe.refund(any())).thenReturn(RefundResult.builder().build());

        service.reconcileOrder(9L);

        verify(stripe).refund(any());
        verify(transitions, never()).applySuccess(anyLong(), anyLong(), any());
    }

    @Test
    void webhookSuccess_appliesItUsingEventPaymentId() {
        when(paymentAttemptRepository.findFirstByProviderNameAndProviderPaymentReferenceOrderByCreatedAtDesc(
                ProviderName.STRIPE, "pi_123")).thenReturn(Optional.of(attempt));
        when(transitions.applySuccess(9L, 6L, "pi_123")).thenReturn(SuccessOutcome.MARKED_PAID);

        service.handleProviderEvent(PaymentEvent.builder()
                .providerName(ProviderName.STRIPE)
                .type(PaymentEventType.PAYMENT_SUCCEEDED)
                .providerPaymentReference("pi_123")
                .providerPaymentId("pi_123")
                .amountMinorUnits(1_680_000L)
                .currency("inr")
                .build());

        verify(transitions).applySuccess(9L, 6L, "pi_123");
    }

    @Test
    void cancelOrderForUser_reconcilesFirstThenCancelsProviderPayment() {
        when(stripe.fetchStatus("pi_123")).thenReturn(
                PaymentStatusResult.builder().state(PaymentStatusResult.State.PENDING).build());
        when(transitions.cancelPendingOrder(9L, false)).thenReturn(new CancellationResult(
                true, List.of(new ProviderPaymentRef(6L, ProviderName.STRIPE, "pi_123")), null));

        service.cancelOrderForUser("buyer@example.com", 9L);

        var calls = inOrder(stripe, transitions);
        calls.verify(stripe).fetchStatus("pi_123");
        calls.verify(transitions).cancelPendingOrder(9L, false);
        calls.verify(stripe).cancel("pi_123");
    }

    @Test
    void cancelOrderForUser_providerCancelFailure_doesNotFailTheCancel() {
        when(stripe.fetchStatus("pi_123")).thenReturn(
                PaymentStatusResult.builder().state(PaymentStatusResult.State.PENDING).build());
        when(transitions.cancelPendingOrder(9L, false)).thenReturn(new CancellationResult(
                true, List.of(new ProviderPaymentRef(6L, ProviderName.STRIPE, "pi_123")), null));
        doThrow(new PaymentProviderException(ProviderName.STRIPE, "already succeeded", false, null))
                .when(stripe).cancel("pi_123");

        service.cancelOrderForUser("buyer@example.com", 9L);

        verify(stripe).cancel("pi_123");
    }

    @Test
    void cancelOrderForUser_whenPaidMeanwhile_isRejected() {
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(1_680_000L, "inr"));
        when(transitions.applySuccess(9L, 6L, "pi_123")).thenReturn(SuccessOutcome.MARKED_PAID);
        when(transitions.cancelPendingOrder(9L, false)).thenReturn(CancellationResult.notCancelled());

        assertThatThrownBy(() -> service.cancelOrderForUser("buyer@example.com", 9L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("already been paid");
        verify(stripe, never()).cancel(any());
    }

    @Test
    void cancelOrderForUser_onNonPendingOrder_isRejectedWithoutProviderCalls() {
        order.setOrderStatus(OrderStatus.PAID.name());

        assertThatThrownBy(() -> service.cancelOrderForUser("buyer@example.com", 9L))
                .isInstanceOf(APIException.class);
        verify(transitions, never()).cancelPendingOrder(anyLong(), anyBoolean());
        verify(stripe, never()).fetchStatus(any());
    }

    @Test
    void expireUnpaidOrders_cancelsProviderPaymentAndSendsAbandonedCartEmail() {
        Cart cart = new Cart();
        cart.setCartId(3L);
        when(orderRepository.findPendingPaymentOrderIdsWithoutLiveHold(any())).thenReturn(List.of(9L));
        when(stripe.fetchStatus("pi_123")).thenReturn(
                PaymentStatusResult.builder().state(PaymentStatusResult.State.PENDING).build());
        when(transitions.cancelPendingOrder(9L, true)).thenReturn(new CancellationResult(
                true, List.of(new ProviderPaymentRef(6L, ProviderName.STRIPE, "pi_123")), cart));

        service.expireUnpaidOrders();

        verify(stripe).cancel("pi_123");
        verify(eventPublisher).publishEvent(any(OnCartReservationExpiredEvent.class));
    }

    @Test
    void expireUnpaidOrders_paidWithoutWebhook_isMarkedPaidNotCancelled() {
        when(orderRepository.findPendingPaymentOrderIdsWithoutLiveHold(any())).thenReturn(List.of(9L));
        when(stripe.fetchStatus("pi_123")).thenReturn(succeeded(1_680_000L, "inr"));
        when(transitions.applySuccess(9L, 6L, "pi_123")).thenReturn(SuccessOutcome.MARKED_PAID);
        when(transitions.cancelPendingOrder(9L, true)).thenReturn(CancellationResult.notCancelled());

        service.expireUnpaidOrders();

        verify(transitions).applySuccess(9L, 6L, "pi_123");
        verify(stripe, never()).cancel(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void syncOrderForUser_isThrottledPerOrder() {
        when(stripe.fetchStatus("pi_123")).thenReturn(
                PaymentStatusResult.builder().state(PaymentStatusResult.State.PENDING).build());

        service.syncOrderForUser("buyer@example.com", 9L);
        service.syncOrderForUser("buyer@example.com", 9L);

        verify(stripe, times(1)).fetchStatus("pi_123");
    }
}
