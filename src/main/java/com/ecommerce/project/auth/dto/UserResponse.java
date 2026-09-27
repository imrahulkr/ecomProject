package com.ecommerce.project.auth.dto;

import java.util.List;

public record UserResponse(
        List<SellerSummaryDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElements,
        Integer totalPages,
        boolean lastPage
) {}
