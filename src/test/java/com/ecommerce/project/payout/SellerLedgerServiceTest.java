package com.ecommerce.project.payout;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.payout.dto.EarningsSummaryDTO;
import com.ecommerce.project.payout.dto.PayoutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerLedgerServiceTest {

    @Mock private SellerLedgerRepository ledgerRepository;
    @Mock private SellerPayoutRepository payoutRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SellerLedgerService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "commissionPercent", 10.0);
        ReflectionTestUtils.setField(service, "returnWindowDays", 7);
        ReflectionTestUtils.setField(service, "currency", "INR");
    }

    private static OrderItem item(Long id, Long sellerId, long price, int qty) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        item.setSellerId(sellerId);
        item.setOrderedProductPriceMinorUnits(price);
        item.setQuantity(qty);
        item.setCurrency("INR");
        return item;
    }

    @Test
    void recordSale_booksSaleAndCommissionPerSellerItem() {
        Order order = new Order();
        order.setOrderId(9L);
        order.setItems(List.of(item(1L, 4L, 560_000L, 2), item(2L, null, 100L, 1)));

        service.recordSale(order);

        ArgumentCaptor<SellerLedgerEntry> saved = ArgumentCaptor.forClass(SellerLedgerEntry.class);
        verify(ledgerRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(SellerLedgerEntry::getEntryType, SellerLedgerEntry::getAmountMinorUnits)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(LedgerEntryType.SALE, 1_120_000L),
                        org.assertj.core.groups.Tuple.tuple(LedgerEntryType.COMMISSION, -112_000L));
    }

    @Test
    void recordRefund_reversesWhatWasBooked() {
        SellerLedgerEntry sale = new SellerLedgerEntry(4L, 9L, 1L, LedgerEntryType.SALE, 1_000L, "INR");
        SellerLedgerEntry commission = new SellerLedgerEntry(4L, 9L, 1L, LedgerEntryType.COMMISSION, -100L, "INR");
        when(ledgerRepository.findByOrderItemIdAndEntryTypeIn(eq(1L), anyCollection())).thenReturn(List.of(sale, commission));

        service.recordRefund(item(1L, 4L, 1_000L, 1));

        ArgumentCaptor<SellerLedgerEntry> saved = ArgumentCaptor.forClass(SellerLedgerEntry.class);
        verify(ledgerRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(SellerLedgerEntry::getEntryType, SellerLedgerEntry::getAmountMinorUnits)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(LedgerEntryType.REFUND, -1_000L),
                        org.assertj.core.groups.Tuple.tuple(LedgerEntryType.COMMISSION_REVERSAL, 100L));
    }

    @Test
    void summary_splitsPendingFromAvailable() {
        when(ledgerRepository.sumUnpaid(4L)).thenReturn(1_500L);
        when(ledgerRepository.sumSettledUnpaid(eq(4L), anyCollection(), anyCollection(), any())).thenReturn(900L);
        when(payoutRepository.sumForSeller(4L)).thenReturn(300L);

        EarningsSummaryDTO summary = service.summary(4L);

        assertThat(summary.availableMinorUnits()).isEqualTo(900L);
        assertThat(summary.pendingMinorUnits()).isEqualTo(600L);
        assertThat(summary.paidOutMinorUnits()).isEqualTo(300L);
    }

    @Test
    void payout_paysEverythingAvailableAndStampsTheEntries() {
        User seller = new User();
        seller.setUserId(4L);
        seller.setUsername("seller");
        SellerLedgerEntry a = new SellerLedgerEntry(4L, 9L, 1L, LedgerEntryType.SALE, 1_000L, "INR");
        SellerLedgerEntry b = new SellerLedgerEntry(4L, 9L, 1L, LedgerEntryType.COMMISSION, -100L, "INR");
        when(userRepository.findById(4L)).thenReturn(Optional.of(seller));
        when(ledgerRepository.findSettledUnpaidForUpdate(eq(4L), anyCollection(), anyCollection(), any())).thenReturn(List.of(a, b));
        when(payoutRepository.save(any(SellerPayout.class))).thenAnswer(inv -> {
            SellerPayout p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });

        PayoutDTO payout = service.payout(4L, 3L, " UTR123 ");

        assertThat(payout.amountMinorUnits()).isEqualTo(900L);
        assertThat(payout.reference()).isEqualTo("UTR123");
        assertThat(a.getPayoutId()).isEqualTo(7L);
        assertThat(b.getPayoutId()).isEqualTo(7L);
    }

    @Test
    void payout_withNothingAvailable_isRejected() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(new User()));
        when(ledgerRepository.findSettledUnpaidForUpdate(eq(4L), anyCollection(), anyCollection(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.payout(4L, 3L, null)).isInstanceOf(APIException.class);
        verify(payoutRepository, never()).save(any());
    }
}
