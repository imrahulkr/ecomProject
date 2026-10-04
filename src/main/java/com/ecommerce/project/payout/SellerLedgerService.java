package com.ecommerce.project.payout;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.order.FulfillmentStatus;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.payout.dto.EarningsSummaryDTO;
import com.ecommerce.project.payout.dto.LedgerEntryDTO;
import com.ecommerce.project.payout.dto.PayoutDTO;
import com.ecommerce.project.payout.dto.SellerBalanceDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Seller earnings: what each paid item earns its seller (price x quantity minus the marketplace
// commission), reversed when the item is refunded, and paid out by an admin once the item can no
// longer be returned. Coupon discounts are marketplace-funded, so they don't reduce what the
// seller earns.
@Service
@RequiredArgsConstructor
public class SellerLedgerService {

    // Item states after which a sale can no longer change.
    private static final Set<FulfillmentStatus> CLOSED = EnumSet.of(FulfillmentStatus.CANCELLED, FulfillmentStatus.RETURNED);
    // Delivered states that settle once the return window has passed.
    private static final Set<FulfillmentStatus> DELIVERED = EnumSet.of(FulfillmentStatus.DELIVERED, FulfillmentStatus.RETURN_REJECTED);

    private final SellerLedgerRepository ledgerRepository;
    private final SellerPayoutRepository payoutRepository;
    private final UserRepository userRepository;

    @Value("${app.marketplace.commission-percent}")
    private double commissionPercent;

    @Value("${app.returns.window-days}")
    private int returnWindowDays;

    @Value("${app.currency}")
    private String currency;

    // ---- Writes (run inside the caller's transaction) ----

    /** Called when an order becomes PAID. */
    public void recordSale(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSellerId() == null) {
                continue;
            }
            long gross = item.getOrderedProductPriceMinorUnits() * item.getQuantity();
            ledgerRepository.save(new SellerLedgerEntry(item.getSellerId(), order.getOrderId(), item.getOrderItemId(),
                    LedgerEntryType.SALE, gross, item.getCurrency()));
            ledgerRepository.save(new SellerLedgerEntry(item.getSellerId(), order.getOrderId(), item.getOrderItemId(),
                    LedgerEntryType.COMMISSION, -commissionOn(gross), item.getCurrency()));
        }
    }

    /** Called when an item is refunded: reverses exactly what recordSale booked for it. */
    public void recordRefund(OrderItem item) {
        List<SellerLedgerEntry> booked = ledgerRepository.findByOrderItemIdAndEntryTypeIn(
                item.getOrderItemId(), EnumSet.of(LedgerEntryType.SALE, LedgerEntryType.COMMISSION));
        for (SellerLedgerEntry entry : booked) {
            LedgerEntryType reversal = entry.getEntryType() == LedgerEntryType.SALE
                    ? LedgerEntryType.REFUND : LedgerEntryType.COMMISSION_REVERSAL;
            ledgerRepository.save(new SellerLedgerEntry(entry.getSellerId(), entry.getOrderId(), entry.getOrderItemId(),
                    reversal, -entry.getAmountMinorUnits(), entry.getCurrency()));
        }
    }

    // ---- Reads ----

    public EarningsSummaryDTO summary(Long sellerId) {
        long unpaid = ledgerRepository.sumUnpaid(sellerId);
        long available = ledgerRepository.sumSettledUnpaid(sellerId, CLOSED, DELIVERED, settlementCutoff());
        return new EarningsSummaryDTO(currency, commissionPercent, returnWindowDays,
                unpaid - available, available, payoutRepository.sumForSeller(sellerId));
    }

    public Page<LedgerEntryDTO> ledger(Long sellerId, Pageable pageable) {
        return ledgerRepository.findBySellerIdOrderByCreatedAtDescIdDesc(sellerId, pageable)
                .map(e -> new LedgerEntryDTO(e.getId(), e.getEntryType(), e.getAmountMinorUnits(), e.getCurrency(),
                        e.getOrderId(), e.getOrderItemId(), e.getPayoutId() != null, e.getCreatedAt()));
    }

    public List<SellerBalanceDTO> balances() {
        List<Long> sellerIds = ledgerRepository.findSellerIdsWithUnpaidEntries();
        Map<Long, User> sellers = userRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        return sellerIds.stream()
                .map(id -> {
                    EarningsSummaryDTO s = summary(id);
                    User seller = sellers.get(id);
                    return new SellerBalanceDTO(id, seller != null ? displayName(seller) : "Seller #" + id,
                            seller != null ? seller.getEmail() : null, s.currency(),
                            s.pendingMinorUnits(), s.availableMinorUnits(), s.paidOutMinorUnits());
                })
                .toList();
    }

    public Page<PayoutDTO> payouts(Long sellerIdOrNull, Pageable pageable) {
        Page<SellerPayout> page = sellerIdOrNull != null
                ? payoutRepository.findBySellerIdOrderByCreatedAtDesc(sellerIdOrNull, pageable)
                : payoutRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, User> sellers = userRepository.findAllById(page.map(SellerPayout::getSellerId).toList()).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        return page.map(p -> toDTO(p, sellers.get(p.getSellerId())));
    }

    // ---- Payout ----

    /**
     * Records that the seller has been paid everything currently available, and marks those
     * ledger entries as paid out. The money itself moves outside this app (bank transfer);
     * reference is that transfer's id.
     */
    @Transactional
    public PayoutDTO payout(Long sellerId, Long adminUserId, String reference) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new APIException("Seller not found"));
        List<SellerLedgerEntry> entries = ledgerRepository.findSettledUnpaidForUpdate(
                sellerId, CLOSED, DELIVERED, settlementCutoff());
        long amount = entries.stream().mapToLong(SellerLedgerEntry::getAmountMinorUnits).sum();
        if (amount <= 0) {
            throw new APIException("Nothing is available to pay out for this seller yet");
        }

        SellerPayout payout = new SellerPayout();
        payout.setSellerId(sellerId);
        payout.setAmountMinorUnits(amount);
        payout.setCurrency(currency);
        payout.setReference(reference == null || reference.isBlank() ? null : reference.trim());
        payout.setCreatedBy(adminUserId);
        SellerPayout saved = payoutRepository.save(payout);
        entries.forEach(e -> e.setPayoutId(saved.getId()));
        return toDTO(saved, seller);
    }

    private long commissionOn(long grossMinorUnits) {
        return Math.round(grossMinorUnits * commissionPercent / 100.0);
    }

    private LocalDateTime settlementCutoff() {
        return LocalDateTime.now().minusDays(returnWindowDays);
    }

    private static PayoutDTO toDTO(SellerPayout p, User seller) {
        return new PayoutDTO(p.getId(), p.getSellerId(), seller != null ? displayName(seller) : null,
                p.getAmountMinorUnits(), p.getCurrency(), p.getReference(), p.getCreatedAt());
    }

    private static String displayName(User user) {
        return user.getName() != null && !user.getName().isBlank() ? user.getName() : user.getUsername();
    }
}
