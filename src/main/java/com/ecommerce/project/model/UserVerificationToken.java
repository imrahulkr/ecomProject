package com.ecommerce.project.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.ecommerce.project.config.AppConstants.VERIFICATION_EXPIRATION_MINUTES;

@Entity
@Table(name = "verificationToken")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserVerificationToken {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private LocalDateTime expiryDate;

    public UserVerificationToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusMinutes(VERIFICATION_EXPIRATION_MINUTES);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

}
