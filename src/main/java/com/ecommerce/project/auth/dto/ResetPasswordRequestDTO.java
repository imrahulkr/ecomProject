package com.ecommerce.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "Token is required.")
        String token,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}
