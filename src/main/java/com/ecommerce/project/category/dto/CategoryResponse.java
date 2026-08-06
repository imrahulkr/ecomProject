package com.ecommerce.project.category.dto;

import java.util.List;

public record CategoryResponse(
        List<CategoryDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElement,
        Integer totalPages,
        boolean lastPage
) {}
