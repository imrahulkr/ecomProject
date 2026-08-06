package com.ecommerce.project.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
import com.ecommerce.project.auth.User;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Store only a hash of the raw token - never the raw value.
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    // All tokens rotated from a single login chain share this id.
    // Lets us detect and revoke an entire chain if a used token is replayed.
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isEspired(){
        return Instant.now().isAfter(this.expiresAt);
    }
}
