package com.ecommerce.project.security.dto;

public record AuthResponse (
        String accessToken,
        long expiresInSecond,
        UserSummary user
){
    public record UserSummary (
            String id,
            String email,
            String name,
            boolean hasPassword,
            java.util.List<String> linkedProviders
    ) {}
}
