package com.ecommerce.project.refund;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.order.FulfillmentStatus;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.order.OrderItemRepository;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payout.SellerLedgerService;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.refund.RefundTransitions.PreparedRefund;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundTransitionsTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private ProductRepository productRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private SellerLedgerService sellerLedgerService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private RefundTransitions transitions;

    private Order order;
    private OrderItem keyboard; // 5,600.00
    private OrderItem phone;    // 11,200.00

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transitions, "entityManager", entityManager);

        order = new Order();
        order.setOrderId(9L);
        order.setOrderStatus(OrderStatus.PAID.name());
        order.setCurrency("INR");
        order.setAmountMinorUnits(1_680_000L);
        keyboard = item(1L, 23L, 560_000L);
        phone = item(2L, 62L, 1_120_000L);
        order.setItems(List.of(keyboard, phone));

        PaymentAttempt paid = new PaymentAttempt(order, ProviderName.STRIPE, 1_680_000L, "INR");
        paid.setId(6L);
        paid.setStatus(PaymentAttemptStatus.SUCCEEDED);
        paid.setProviderPaymentReference("pi_123");
        paid.setProviderPaymentId("pi_123");

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(keyboard));
        when(orderItemRepository.findById(2L)).thenReturn(Optional.of(phone));
        when(orderRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findByOrder_OrderIdAndStatusIn(eq(9L), any())).thenReturn(List.of(paid));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
    }

    private OrderItem item(Long id, Long productId, long price) {
        Product product = new Product();
        product.setProductId(productId);
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setOrderedProductPriceMinorUnits(price);
        item.setCurrency("INR");
        item.setSellerId(4L);
        item.setFulfillmentStatus(FulfillmentStatus.PENDING);
        return item;
    }

    @Test
    void cancellingAnItem_refundsItsPriceRestocksAndReversesSellerEarnings() {
        PreparedRefund refund = transitions.prepare(1L, null, FulfillmentStatus.CANCELLED, "Cancelled by the seller");

        assertThat(refund.amountMinorUnits()).isEqualTo(560_000L);
        assertThat(refund.providerPaymentId()).isEqualTo("pi_123");
        assertThat(keyboard.getFulfillmentStatus()).isEqualTo(FulfillmentStatus.CANCELLED);
        assertThat(keyboard.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        verify(productRepository).incrementStock(23L, 1);
        verify(sellerLedgerService).recordRefund(keyboard);
        ArgumentCaptor<Refund> saved = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(saved.getValue().getPaymentAttemptId()).isEqualTo(6L);
    }

    @Test
    void couponDiscount_isSharedProRata() {
        order.setDiscountMinorUnits(168_000L);
        order.setAmountMinorUnits(1_512_000L); // 10% coupon

        PreparedRefund refund = transitions.prepare(1L, null, FulfillmentStatus.CANCELLED, "x");

        // 5,600 of 16,800 merchandise -> one third of what was actually paid.
        assertThat(refund.amountMinorUnits()).isEqualTo(504_000L);
    }

    @Test
    void lastItemRefunded_takesTheRemainderIncludingShipping() {
        order.setShippingMinorUnits(4_900L);
        order.setAmountMinorUnits(1_684_900L);
        keyboard.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
        when(refundRepository.sumCommittedForOrder(9L)).thenReturn(560_000L);

        PreparedRefund refund = transitions.prepare(2L, null, FulfillmentStatus.CANCELLED, "x");

        assertThat(refund.amountMinorUnits()).isEqualTo(1_124_900L);
    }

    @Test
    void approvingAReturn_refundsTheItem() {
        keyboard.setFulfillmentStatus(FulfillmentStatus.RETURN_REQUESTED);

        PreparedRefund refund = transitions.prepare(1L, null, FulfillmentStatus.RETURNED, "Return approved");

        assertThat(keyboard.getFulfillmentStatus()).isEqualTo(FulfillmentStatus.RETURNED);
        assertThat(refund.amountMinorUnits()).isEqualTo(560_000L);
    }

    @Test
    void shippedItem_cannotBeCancelled() {
        keyboard.setFulfillmentStatus(FulfillmentStatus.SHIPPED);

        assertThatThrownBy(() -> transitions.prepare(1L, null, FulfillmentStatus.CANCELLED, "x"))
                .isInstanceOf(APIException.class);
        verify(productRepository, never()).incrementStock(anyLong(), any());
        verify(refundRepository, never()).save(any());
    }

    @Test
    void unpaidOrder_hasNothingToRefund() {
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());

        assertThatThrownBy(() -> transitions.prepare(1L, null, FulfillmentStatus.CANCELLED, "x"))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("not paid");
    }

    @Test
    void sellerScopedLookup_hidesOtherSellersItems() {
        when(orderItemRepository.findByOrderItemIdAndSellerId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transitions.prepare(1L, 99L, FulfillmentStatus.CANCELLED, "x"))
                .isInstanceOf(com.ecommerce.project.exceptions.ResourceNotFoundException.class);
    }
}
