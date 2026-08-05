package com.ecommerce.project.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import com.ecommerce.project.auth.User;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = @UniqueConstraint( columnNames = {"provider", "provider_user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String provider;   // "google", "github"

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @PrePersist
    void onCreate() {
        linkedAt = Instant.now();
    }
}
