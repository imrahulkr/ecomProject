package com.ecommerce.project.coupon.dto;

import java.util.List;

public record CouponPageResponse(
        List<CouponDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElements,
        Integer totalPages,
        boolean lastPage
) {}
