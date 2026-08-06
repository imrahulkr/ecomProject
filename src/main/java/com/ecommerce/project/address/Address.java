package com.ecommerce.project.address;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.ecommerce.project.auth.User;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long addressId;

    @NotBlank
    @Size(min = 5, max = 50, message = "Street name must be atleast 5 characters")
    private String street;

    @NotBlank
    @Size(min = 4,  message = "Building name must be atleast 4 characters")
    private String buildingName;

    @NotBlank
    @Size(min = 4,  message = "City name must be atleast 4 characters")
    private String city;

    @NotBlank
    @Size(min = 3,  message = "City name must be atleast 3 characters")
    private String state;

    @NotBlank
    @Size(min = 3,  message = "Country name must be atleast 3 characters")
    private String country;

    @NotBlank
    @Size(min = 6,  message = "Pincode must be of size 6 characters")
    private String pincode;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Address(String street, String buildingName, String city, String state, String country, String pincode) {
        this.street = street;
        this.buildingName = buildingName;
        this.city = city;
        this.state = state;
        this.country = country;
        this.pincode = pincode;
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
}
