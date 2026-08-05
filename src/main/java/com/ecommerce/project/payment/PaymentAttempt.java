package com.ecommerce.project.payment;

import com.ecommerce.project.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// One row per attempt, not just per successful payment - the checkout endpoint and every
// explicit retry each create their own row, so the full history of what was tried survives
// even when nothing ever succeeds.
@Entity
@Table(name = "payment_attempts")
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_name", nullable = false, length = 32)
    private ProviderName providerName;

    @Column(name = "provider_payment_reference")
    private String providerPaymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentAttemptStatus status;

    // columnDefinition must match the migration's TEXT column exactly - without it Hibernate's
    // ddl-auto=update (which still runs over Flyway-owned tables too) silently narrows this back
    // to its default VARCHAR(255) on the next boot that happens to touch this table.
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false, length = 8)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentAttempt(Order order, ProviderName providerName, long amountMinorUnits, String currency) {
        this.order = order;
        this.providerName = providerName;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.status = PaymentAttemptStatus.INITIATED;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = this.updatedAt;
        }
    }
}
