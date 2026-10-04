package com.ecommerce.project.product.dto;

import java.util.List;

public record ProductResponse(
        List<ProductDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElement,
        Integer totalPages,
        boolean lastPage
) {}
