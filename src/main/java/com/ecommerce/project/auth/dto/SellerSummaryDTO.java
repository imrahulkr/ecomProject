package com.ecommerce.project.auth.dto;

public record SellerSummaryDTO(
        Long userId,
        String username,
        String email,
        String name
) {}
