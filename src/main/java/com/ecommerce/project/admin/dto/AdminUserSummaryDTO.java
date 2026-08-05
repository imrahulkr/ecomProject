package com.ecommerce.project.admin.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

// Deliberately not the existing auth.dto.UserDTO - that one includes the password hash, which
// has no business being serialized into any HTTP response, admin-only or not.
@Value
@Builder
public class AdminUserSummaryDTO {
    Long userId;
    String username;
    String email;
    String name;
    boolean enabled;
    Set<String> roles;
}
