package com.ecommerce.project.address.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private Long addressId;

    @NotBlank
    @Size(min = 5, max = 50, message = "Street name must be atleast 5 characters")
    private String street;

    @NotBlank
    @Size(min = 4, message = "Building name must be atleast 4 characters")
    private String buildingName;

    @NotBlank
    @Size(min = 4, message = "City name must be atleast 4 characters")
    private String city;

    @NotBlank
    @Size(min = 3, message = "State name must be atleast 3 characters")
    private String state;

    @NotBlank
    @Size(min = 3, message = "Country name must be atleast 3 characters")
    private String country;

    @NotBlank
    @Size(min = 6, message = "Pincode must be of size 6 characters")
    private String pincode;

    private Instant createdAt;
    private Instant updatedAt;
}
