package com.ecommerce.project.review.dto;

import java.util.List;

public record ReviewPageResponse(
        List<ReviewDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElements,
        Integer totalPages,
        boolean lastPage
) {}
