package com.ecommerce.project.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

import static com.ecommerce.project.config.AppConstants.PASSWORD_RESET_EXPIRATION_MINUTES;

@Entity
@Table(name = "password_reset_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PasswordResetToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.expirationDate = LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRATION_MINUTES);
    }
    public boolean isExpired() {
        return  LocalDateTime.now().isAfter(this.expirationDate);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
