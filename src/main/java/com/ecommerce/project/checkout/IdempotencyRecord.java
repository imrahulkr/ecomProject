package com.ecommerce.project.checkout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Deliberately no entity-typed fields (user/order are plain ids, not @ManyToOne) - this table is
// looked up and merged outside of any wider persistence context, and entities with bidirectional
// associations (e.g. User/Cart) are unsafe to detach-then-merge in this codebase; see
// InventoryServiceImpl's notes on the same issue.
@Entity
@Table(name = "idempotency_records")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotencyEndpoint endpoint;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IdempotencyStatus status;

    @Column(name = "order_id")
    private Long orderId;

    // columnDefinition, not @Lob: Hibernate maps a @Lob String to Postgres OID by default, which
    // doesn't match the plain TEXT column the migration creates (see EmailLog's errorMessage /
    // metadataJson columns for the same established pattern in this codebase).
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IdempotencyRecord(Long userId, String idempotencyKey, IdempotencyEndpoint endpoint, String requestHash) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.endpoint = endpoint;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.IN_PROGRESS;
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
