package com.ecommerce.project.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewModerationRequest(
        @NotNull Boolean hidden,
        @Size(max = 500) String reason
) {}
