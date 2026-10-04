package com.ecommerce.project.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemDTO(
	@NotNull Long productId,
	@NotNull @Positive Integer quantity
) {}
