package com.ecommerce.project.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.ecommerce.project.address.Address;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.security.OAuthAccount;
import com.ecommerce.project.product.Product;

@Entity
@Data
@NoArgsConstructor
@Table(name = "users",  uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")})
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    private String name;
    @NotBlank
    @Size(min = 1, max = 20)
    @Column(name = "username")
    private String username;

    @NotBlank
    @Size(min = 1, max = 50)
    @Email
    @Column(name = "email")
    private String email;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Getter
    @Setter
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OAuthAccount> oAuthAccounts = new ArrayList<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = false; // set true once email is verified.
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean hasPassword(){
        return password != null;
    }

    @Setter
    @Getter
    @ManyToMany(cascade = {CascadeType.PERSIST,  CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @ToString.Exclude
    @OneToOne(mappedBy = "user", cascade = {CascadeType.PERSIST,  CascadeType.MERGE}, orphanRemoval = true)
    private Cart cart;


    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                orphanRemoval = true)
    private Set<Product> products;

    @Setter
    @Getter
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
//    @JoinTable(name = "user_address", joinColumns = @JoinColumn(name = "user_id"),
//                inverseJoinColumns = @JoinColumn(name = "address_id"))
    private List<Address> addresses = new ArrayList<>();


}
