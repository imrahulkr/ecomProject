package com.ecommerce.project.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.ecommerce.project.config.AppConstants.VERIFICATION_EXPIRATION_MINUTES;

@Entity
@Table(name = "verificationToken")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserVerificationToken {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserVerificationToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusMinutes(VERIFICATION_EXPIRATION_MINUTES);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
