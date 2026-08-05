package com.ecommerce.project.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplySellerRequest {

    @NotBlank
    @Size(min = 3, max = 255)
    private String businessName;

    @Size(max = 2000)
    private String businessDescription;
}
