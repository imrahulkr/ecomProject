package com.ecommerce.project.order.dto;

import java.util.List;

public record OrderResponse(
        List<OrderDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElement,
        Integer totalPages,
        boolean lastPage
) {}
