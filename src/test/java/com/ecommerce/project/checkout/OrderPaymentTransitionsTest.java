package com.ecommerce.project.checkout;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.cart.CartService;
import com.ecommerce.project.checkout.OrderPaymentTransitions.CancellationResult;
import com.ecommerce.project.checkout.OrderPaymentTransitions.ProviderPaymentRef;
import com.ecommerce.project.checkout.OrderPaymentTransitions.SuccessOutcome;
import com.ecommerce.project.inventory.InventoryService;
import com.ecommerce.project.inventory.StockReservation;
import com.ecommerce.project.notification.email.event.OnOrderPaidEvent;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payout.SellerLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentTransitionsTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private CartService cartService;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SellerLedgerService sellerLedgerService;

    @InjectMocks
    private OrderPaymentTransitions transitions;

    private Order order;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(9L);
        order.setEmail("buyer@example.com");
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());

        attempt = attempt(6L, PaymentAttemptStatus.INITIATED, "pi_123");

        when(orderRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(order));
    }

    private PaymentAttempt attempt(Long id, PaymentAttemptStatus status, String reference) {
        PaymentAttempt a = new PaymentAttempt(order, ProviderName.STRIPE, 1_680_000L, "INR");
        a.setId(id);
        a.setStatus(status);
        a.setProviderPaymentReference(reference);
        return a;
    }

    @Test
    void applySuccess_onPendingOrder_marksPaidConfirmsStockAndNotifies() {
        when(paymentAttemptRepository.findById(6L)).thenReturn(Optional.of(attempt));

        SuccessOutcome outcome = transitions.applySuccess(9L, 6L, "pi_123");

        assertThat(outcome).isEqualTo(SuccessOutcome.MARKED_PAID);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID.name());
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        verify(inventoryService).confirmReservationsForOrder(9L);
        verify(sellerLedgerService).recordSale(order);
        verify(eventPublisher).publishEvent(any(OnOrderPaidEvent.class));
        assertThat(attempt.getProviderPaymentId()).isEqualTo("pi_123");
    }

    @Test
    void applySuccess_afterFailedAttempt_stillMarksPaid() {
        // Stripe lets the customer retry the same PaymentIntent after a decline.
        attempt.setStatus(PaymentAttemptStatus.FAILED);
        when(paymentAttemptRepository.findById(6L)).thenReturn(Optional.of(attempt));

        assertThat(transitions.applySuccess(9L, 6L, "pi_123")).isEqualTo(SuccessOutcome.MARKED_PAID);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID.name());
    }

    @Test
    void applySuccess_alreadyRecorded_hasNoSideEffects() {
        attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
        order.setOrderStatus(OrderStatus.PAID.name());
        when(paymentAttemptRepository.findById(6L)).thenReturn(Optional.of(attempt));

        assertThat(transitions.applySuccess(9L, 6L, "pi_123")).isEqualTo(SuccessOutcome.ALREADY_RECORDED);
        verifyNoInteractions(inventoryService, eventPublisher, sellerLedgerService);
    }

    @Test
    void applySuccess_onCancelledOrder_requiresRefund() {
        order.setOrderStatus(OrderStatus.CANCELLED.name());
        attempt.setStatus(PaymentAttemptStatus.CANCELLED);
        when(paymentAttemptRepository.findById(6L)).thenReturn(Optional.of(attempt));

        assertThat(transitions.applySuccess(9L, 6L, "pi_123")).isEqualTo(SuccessOutcome.REFUND_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.REFUND_PENDING);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED.name());
        verifyNoInteractions(inventoryService, eventPublisher, sellerLedgerService);
    }

    @Test
    void applySuccess_onOrderPaidByAnotherAttempt_requiresRefund() {
        order.setOrderStatus(OrderStatus.PAID.name());
        when(paymentAttemptRepository.findById(6L)).thenReturn(Optional.of(attempt));

        assertThat(transitions.applySuccess(9L, 6L, "pi_123")).isEqualTo(SuccessOutcome.REFUND_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.REFUND_PENDING);
    }

    @Test
    void cancelPendingOrder_expired_cancelsOrderExpiresStockAndRetiresOpenAttempts() {
        Cart cart = new Cart();
        cart.setCartId(3L);
        StockReservation reservation = new StockReservation();
        reservation.setCart(cart);
        PaymentAttempt withoutReference = attempt(7L, PaymentAttemptStatus.FAILED, null);
        when(inventoryService.expireReservationsForOrder(9L)).thenReturn(List.of(reservation));
        when(paymentAttemptRepository.findByOrder_OrderIdAndStatusIn(eq(9L), any()))
                .thenReturn(List.of(attempt, withoutReference));

        CancellationResult result = transitions.cancelPendingOrder(9L, true);

        assertThat(result.cancelled()).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(result.expiredCart()).isSameAs(cart);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CANCELLED);
        // Never reached the provider - nothing to cancel, keeps its FAILED history.
        assertThat(withoutReference.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(result.openPayments()).containsExactly(new ProviderPaymentRef(6L, ProviderName.STRIPE, "pi_123"));
        verify(inventoryService, never()).releaseReservationsForOrder(anyLong());
    }

    @Test
    void cancelPendingOrder_byCustomer_releasesStock() {
        when(paymentAttemptRepository.findByOrder_OrderIdAndStatusIn(eq(9L), any())).thenReturn(List.of());

        CancellationResult result = transitions.cancelPendingOrder(9L, false);

        assertThat(result.cancelled()).isTrue();
        assertThat(result.expiredCart()).isNull();
        verify(inventoryService).releaseReservationsForOrder(9L);
        verify(inventoryService, never()).expireReservationsForOrder(anyLong());
    }

    @Test
    void cancelPendingOrder_onPaidOrder_changesNothing() {
        order.setOrderStatus(OrderStatus.PAID.name());

        CancellationResult result = transitions.cancelPendingOrder(9L, true);

        assertThat(result.cancelled()).isFalse();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID.name());
        verifyNoInteractions(inventoryService, paymentAttemptRepository);
    }
}
