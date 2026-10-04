package com.ecommerce.project.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRequest(
        @NotBlank @Size(max = 2000) String reply
) {}
