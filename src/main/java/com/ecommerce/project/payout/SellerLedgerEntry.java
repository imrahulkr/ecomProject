package com.ecommerce.project.payout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Append-only record of what a seller has earned/owes. Entries are never edited except to stamp
// payoutId when they're paid out, so the balance is always the sum of the unpaid rows.
@Entity
@Table(name = "seller_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SellerLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 32)
    private LedgerEntryType entryType;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "payout_id")
    private Long payoutId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    SellerLedgerEntry(Long sellerId, Long orderId, Long orderItemId, LedgerEntryType entryType, long amountMinorUnits, String currency) {
        this.sellerId = sellerId;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.entryType = entryType;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
