package com.ecommerce.project.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnRequestDTO(@NotBlank(message = "Tell us why you're returning this item") @Size(max = 1000) String reason) {}
