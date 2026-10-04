package com.ecommerce.project.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max=20, message="Password must be at least 8 characters")
        String password,
        @NotBlank  @Size(min = 3, max = 20)
        String username,
        String name,
        Set<String> role
) {}

//@Data
//public class SignupRequest {
//    @NotBlank
//    @Size(min = 3, max = 20)
//    private String username;
//    @NotBlank
//    @Size(max = 50)
//    @Email
//    private String email;
//    @NotBlank
//    @Size(min = 8, max = 40)
//    private String password;
//
//    private Set<String> role;
//}
