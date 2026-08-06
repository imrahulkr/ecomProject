package com.ecommerce.project.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplySellerRequest(
        @NotBlank
        @Size(min = 3, max = 255)
        String businessName,

        @Size(max = 2000)
        String businessDescription
) {}
