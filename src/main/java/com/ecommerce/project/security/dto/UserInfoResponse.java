package com.ecommerce.project.security.dto;

import java.util.List;

public record UserInfoResponse(Long id, String jwtToken, String email, String username, List<String> roles) {

    public UserInfoResponse(Long id, String email, String username, List<String> roles, String jwtToken) {
        this(id, jwtToken, email, username, roles);
    }

    public UserInfoResponse(Long id, String email, String username, List<String> roles) {
        this(id, null, email, username, roles);
    }

    public UserInfoResponse(Long id, String username, List<String> roles) {
        this(id, null, null, username, roles);
    }
}
