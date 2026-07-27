package com.ecommerce.project.security.dto;

import java.util.List;

public class UserInfoResponse {
    private Long id;
    private String jwtToken;
    private String email;
    private String username;
    private List<String> roles;

    public UserInfoResponse(Long id, String email, String username, List<String> roles, String jwtToken) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.jwtToken = jwtToken;
        this.roles = roles;
    }

    public UserInfoResponse(Long id, String email, String username, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public UserInfoResponse(Long id, String username, List<String> roles) {
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}


