package com.ecommerce.project.auth.dto;

import java.util.List;

public record UserResponse(
        List<UserDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElements,
        Integer totalPages,
        boolean lastPage
) {}
