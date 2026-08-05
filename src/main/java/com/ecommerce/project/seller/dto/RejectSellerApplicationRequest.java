package com.ecommerce.project.seller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectSellerApplicationRequest {

    @NotBlank
    private String reason;
}
