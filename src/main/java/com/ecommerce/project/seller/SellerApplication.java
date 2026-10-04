package com.ecommerce.project.seller;

import com.ecommerce.project.auth.User;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Holding ROLE_SELLER and being APPROVED are deliberately two separate pieces of state: this
// row is the audit trail and the gate an admin decision moves through; the role grant is just
// a side effect SellerApplicationServiceImpl.approve() performs at the same time. Revoking the
// role later (a general admin capability, not specific to this flow) doesn't have to touch or
// invalidate this record.
@Entity
@Table(name = "seller_applications")
@Getter
@Setter
@NoArgsConstructor
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SellerApplicationStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by_admin_id")
    private Long decidedByAdminId;

    public SellerApplication(User user, String businessName, String businessDescription) {
        this.user = user;
        this.businessName = businessName;
        this.businessDescription = businessDescription;
        this.status = SellerApplicationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.appliedAt = Instant.now();
    }
}
