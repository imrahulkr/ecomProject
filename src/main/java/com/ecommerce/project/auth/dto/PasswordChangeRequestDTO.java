package com.ecommerce.project.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequestDTO(
        @NotBlank(message = "Current Password is required")
        String currentPassword,

        @NotBlank(message = "New Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}
