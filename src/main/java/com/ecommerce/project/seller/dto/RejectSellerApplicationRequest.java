package com.ecommerce.project.seller.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectSellerApplicationRequest(@NotBlank String reason) {}
